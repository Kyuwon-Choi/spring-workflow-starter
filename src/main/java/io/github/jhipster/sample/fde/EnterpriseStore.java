package io.github.jhipster.sample.fde;

import static io.github.jhipster.sample.fde.EnterpriseRecords.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class EnterpriseStore {
    @PersistenceContext EntityManager em;
    public Optional<Member> member(String login) { return Optional.ofNullable(em.find(Member.class, login)); }
    public Tenant lock(String tenant) { return em.find(Tenant.class, tenant, LockModeType.PESSIMISTIC_WRITE); }
    public void refresh(Member member) { em.refresh(member); }
    public void save(Object entity) { em.persist(entity); em.flush(); }
    public void flush() { em.flush(); }
    public List<Member> members(String tenant) { return em.createQuery("from EnterpriseMember where tenant=:tenant order by login", Member.class).setParameter("tenant", tenant).getResultList(); }
    public List<Project> projects(String tenant) { return em.createQuery("from EnterpriseProject where tenant=:tenant order by code", Project.class).setParameter("tenant", tenant).getResultList(); }
    public Optional<Project> project(String tenant, String code) { return projects(tenant).stream().filter(p -> p.code.equals(code)).findFirst(); }
    public List<Rule> rules(String tenant) { return em.createQuery("from EnterpriseRule where tenant=:tenant order by effectiveAt desc", Rule.class).setParameter("tenant", tenant).getResultList(); }
    public List<Delegation> delegations(String tenant) { return em.createQuery("from EnterpriseDelegation where tenant=:tenant order by id desc", Delegation.class).setParameter("tenant", tenant).getResultList(); }
    public List<ConfigAudit> audits(String tenant) { return em.createQuery("from EnterpriseConfigAudit where tenant=:tenant order by id desc", ConfigAudit.class).setParameter("tenant", tenant).setMaxResults(100).getResultList(); }
    public Optional<Rule> ruleAt(String tenant, Instant now) { return rules(tenant).stream().filter(r -> !r.effectiveAt.isAfter(now)).findFirst(); }
    public Optional<Rule> rule(String tenant, String key) { return rules(tenant).stream().filter(r -> r.key.equals(key)).findFirst(); }
}
