package io.github.jhipster.sample.kit;

import io.github.jhipster.sample.security.AuthoritiesConstants;
import io.github.jhipster.sample.security.SecurityUtils;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class KitService {
    public record AssetView(Long id, String code, String name) {}
    public record RequestView(Long id, Long assetId, String assetCode, String title, String ownerLogin,
                              KitRequest.State state, Long version, Instant createdAt) {}
    public record AuditView(Long id, String actor, String action, String fromState, String toState, Instant occurredAt) {}

    private final KitAssetRepository assets;
    private final KitRequestRepository requests;
    private final KitAuditRepository events;

    public KitService(KitAssetRepository assets, KitRequestRepository requests, KitAuditRepository events) {
        this.assets = assets;
        this.requests = requests;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<AssetView> assets() {
        return assets.findAll(PageRequest.of(0, 100, Sort.by("code"))).map(this::view).getContent();
    }

    public AssetView createAsset(String code, String name) {
        return view(assets.saveAndFlush(new KitAsset(code, name.trim())));
    }

    @Transactional(readOnly = true)
    public List<RequestView> requests(int page, int size) {
        var paging = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        var result = isAdmin() ? requests.findAll(paging) : requests.findByOwnerLogin(actor(), paging);
        return result.map(this::view).getContent();
    }

    @Transactional(readOnly = true)
    public RequestView get(Long id) {
        return view(accessible(id, false));
    }

    public RequestView create(Long assetId, String title) {
        var asset = assets.findById(assetId).orElseThrow(() -> notFound());
        var request = requests.saveAndFlush(new KitRequest(asset, title.trim(), actor()));
        events.saveAndFlush(new KitAuditEvent(request, actor(), "CREATED", null));
        return view(request);
    }

    public RequestView transition(Long id, KitRequest.State target, Long expectedVersion) {
        var request = accessible(id, true);
        if (!request.version.equals(expectedVersion)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request changed; reload before retrying");
        }
        var previous = request.state;
        boolean allowed = (previous == KitRequest.State.OPEN && target == KitRequest.State.IN_PROGRESS)
            || (previous == KitRequest.State.IN_PROGRESS && target == KitRequest.State.DONE);
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Transition is not allowed");
        }
        request.state = target;
        requests.saveAndFlush(request);
        events.saveAndFlush(new KitAuditEvent(request, actor(), "TRANSITION", previous));
        return view(request);
    }

    @Transactional(readOnly = true)
    public List<AuditView> history(Long id) {
        accessible(id, false);
        return events.findByRequestIdOrderByIdAsc(id).stream()
            .map(e -> new AuditView(e.id, e.actor, e.action, e.fromState, e.toState, e.occurredAt)).toList();
    }

    private KitRequest accessible(Long id, boolean lock) {
        var request = (lock ? requests.findForUpdate(id) : requests.findById(id)).orElseThrow(() -> notFound());
        // Use the same response for absent and inaccessible objects.
        if (!isAdmin() && !request.ownerLogin.equals(actor())) throw notFound();
        return request;
    }

    private String actor() {
        return SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private boolean isAdmin() {
        return SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN);
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
    }

    private AssetView view(KitAsset asset) { return new AssetView(asset.getId(), asset.getCode(), asset.getName()); }

    private RequestView view(KitRequest request) {
        return new RequestView(request.id, request.asset.getId(), request.asset.getCode(), request.title,
            request.ownerLogin, request.state, request.version, request.createdAt);
    }
}
