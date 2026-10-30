#!/usr/bin/env python3
"""Run against the local practice server. Adds synthetic examples; never prints JWTs."""
import json
import uuid
from urllib.error import HTTPError
from urllib.request import Request, urlopen
from urllib.parse import urlencode

BASE = 'http://127.0.0.1:8080'

def call(method, path, payload=None, token=None, expected=200, text=False):
    headers = {'Content-Type': 'application/json'}
    if token: headers['Authorization'] = 'Bearer ' + token
    request = Request(BASE+path, data=None if payload is None else json.dumps(payload).encode(), headers=headers, method=method)
    try:
        with urlopen(request, timeout=15) as r: status, body = r.status, r.read()
    except HTTPError as e: status, body = e.code, e.read()
    assert status == expected, (method, path, status, expected)
    return body.decode('utf-8') if text else json.loads(body) if body else None

def login(name):
    return call('POST','/api/authenticate',{'username':name,'password':name,'rememberMe':False})['id_token']

def main():
    call('GET','/api/fde/catalog',expected=401)
    regular, admin = login('user'), login('admin')
    catalog=call('GET','/api/fde/catalog',token=regular)
    assert {d['id'] for d in catalog} == {'support','procurement','inventory'}
    suffix=uuid.uuid4().hex[:10]
    def create(domain, kind, data, token=regular):
        return call('POST',f'/api/fde/{domain}/{kind}/objects',{'data':data},token,201)
    def act(domain, kind, item, action, data=None, token=regular, expected=200, version=None):
        return call('POST',f'/api/fde/{domain}/{kind}/objects/{item["id"]}/actions/{action}',
                    {'expectedVersion':item['version'] if version is None else version,'data':data or {}},token,expected)
    customer=create('support','customer',{'code':'C-'+suffix,'name':'Example customer'})
    ticket=create('support','ticket',{'title':'Synthetic inquiry '+suffix,'customer':customer['id'],'priority':'HIGH'})
    act('support','ticket',ticket,'close',expected=409)
    answered=act('support','ticket',ticket,'reply',{'response':'Synthetic answer'})
    closed=act('support','ticket',answered,'close')
    assert closed['state']=='CLOSED'
    history=call('GET',f'/api/fde/support/ticket/objects/{ticket["id"]}/history',token=regular)
    assert len(history)==3 and history[0]['action']=='close'
    private=create('support','ticket',{'title':'Admin-owned example','customer':customer['id'],'priority':'LOW'},admin)
    call('GET',f'/api/fde/support/ticket/objects/{private["id"]}',token=regular,expected=404)
    supplier=create('procurement','supplier',{'code':'V-'+suffix,'name':'Example supplier'})
    purchase=create('procurement','purchase',{'title':'Synthetic purchase','supplier':supplier['id'],'amount':300})
    pending=act('procurement','purchase',purchase,'submit')
    act('procurement','purchase',pending,'approve',expected=403)
    approved=act('procurement','purchase',pending,'approve',token=admin)
    assert approved['state']=='APPROVED'
    stock=create('inventory','item',{'sku':'S-'+suffix,'name':'Example item','quantity':10,'category':'RAW'})
    act('inventory','item',stock,'adjust',{'delta':-11},expected=409)
    adjusted=act('inventory','item',stock,'adjust',{'delta':2})
    assert adjusted['data']['quantity']==12
    act('inventory','item',adjusted,'adjust',{'delta':1},version=0,expected=409)
    csv=f'sku,name,quantity,category\nB1-{suffix},Imported one,2,RAW\nB2-{suffix},Imported two,4,FINISHED\n'
    preview=call('POST','/api/fde/inventory/item/import/preview',{'csv':csv},regular)
    assert preview['valid'] and len(preview['rows'])==2
    imported=call('POST','/api/fde/inventory/item/import',{'rows':preview['rows']},regular,201)
    assert len(imported)==2
    filtered=call('GET','/api/fde/inventory/item/objects?'+urlencode({'q':suffix}),token=regular)
    assert filtered['total']==3
    exported=call('GET','/api/fde/inventory/item/export?'+urlencode({'q':suffix}),token=regular,text=True)
    assert 'quantity' in exported and 'B1-'+suffix in exported and 'owner' not in exported
    print('PASS: same engine across three domains; relationships, different policies, typed CSV preview/import/export, search, actions, extensions, version conflicts, audit and owner isolation.')
    print(json.dumps({'support':ticket['id'],'procurement':purchase['id'],'inventory':stock['id'],'batch_count':len(imported)},ensure_ascii=False))

if __name__ == '__main__': main()
