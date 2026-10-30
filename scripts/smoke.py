#!/usr/bin/env python3
"""Exercise the locally running demo with its public rehearsal accounts; never print tokens."""
import json
from urllib.error import HTTPError
from urllib.request import Request, urlopen

BASE = "http://127.0.0.1:8080"


def call(method, path, payload=None, token=None, expected=200):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    request = Request(BASE + path, data=None if payload is None else json.dumps(payload).encode(), headers=headers, method=method)
    try:
        with urlopen(request, timeout=15) as response:
            status, body = response.status, response.read()
    except HTTPError as error:
        status, body = error.code, error.read()
    assert status == expected, (method, path, status, expected)
    return json.loads(body) if body else None


def login(username, password):
    return call("POST", "/api/authenticate", {"username": username, "password": password, "rememberMe": False})["id_token"]


call("GET", "/api/kit/requests", expected=401)
call("GET", "/api/kit/requests", token="invalid-token", expected=401)
call("POST", "/api/authenticate", {"username": "user", "password": "incorrect"}, expected=401)
admin = login("admin", "admin")
regular = login("user", "user")
asset = call("GET", "/api/kit/assets", token=regular)[0]
item = call("POST", "/api/kit/requests", {"assetId": asset["id"], "title": "로컬 검증: 센서 점검"}, token=regular, expected=201)
assert item["ownerLogin"] == "user" and item["state"] == "OPEN"
retrieved = call("GET", f'/api/kit/requests/{item["id"]}', token=regular)
assert retrieved["assetId"] == asset["id"] and retrieved["assetCode"] == asset["code"]
call("POST", f'/api/kit/requests/{item["id"]}/actions', {"target": "DONE", "expectedVersion": 0}, token=regular, expected=409)
started = call("POST", f'/api/kit/requests/{item["id"]}/actions', {"target": "IN_PROGRESS", "expectedVersion": 0}, token=regular)
call("POST", f'/api/kit/requests/{item["id"]}/actions', {"target": "DONE", "expectedVersion": 0}, token=regular, expected=409)
done = call("POST", f'/api/kit/requests/{item["id"]}/actions', {"target": "DONE", "expectedVersion": started["version"]}, token=admin)
assert done["state"] == "DONE"
history = call("GET", f'/api/kit/requests/{item["id"]}/history', token=regular)
assert len(history) == 3 and history[-1]["actor"] == "admin"
private = call("POST", "/api/kit/requests", {"assetId": asset["id"], "title": "로컬 검증: 관리자 소유 요청"}, token=admin, expected=201)
call("GET", f'/api/kit/requests/{private["id"]}', token=regular, expected=404)
assert all(entry["ownerLogin"] == "user" for entry in call("GET", "/api/kit/requests", token=regular))
call("POST", "/api/kit/requests", {"assetId": asset["id"], "title": "spoof", "ownerLogin": "admin"}, token=regular, expected=400)
print("PASS: real JWT login, invalid authentication, linked objects, owner isolation, state transitions, stale-version conflict, audit history, protected-field rejection.")
print("Created local rehearsal request IDs:", item["id"], private["id"])
