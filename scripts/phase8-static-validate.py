#!/usr/bin/env python3
from pathlib import Path
from lxml import etree
import yaml, json, hashlib, re, sys, subprocess

root = Path(__file__).resolve().parent.parent
errors = []
checks = []

def ok(name, detail=''):
    checks.append((name, 'PASS', detail))

def fail(name, detail):
    checks.append((name, 'FAIL', detail))
    errors.append((name, detail))

poms = [
    root / 'pom.xml',
    root / 'shopops-admin/pom.xml',
    root / 'shopops-common/pom.xml',
    root / 'shopops-commerce-mcp-server/pom.xml',
]
for p in poms:
    try:
        etree.parse(str(p))
        ok(f'XML {p.relative_to(root)}', 'well-formed')
    except Exception as ex:
        fail(f'XML {p.relative_to(root)}', str(ex))

ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
rootpom = etree.parse(str(root / 'pom.xml'))
modules = rootpom.xpath('//m:modules/m:module/text()', namespaces=ns)
expected_modules = ['shopops-common', 'shopops-commerce-mcp-server', 'shopops-admin']
if modules == expected_modules:
    ok('root modules', str(modules))
else:
    fail('root modules', f'{modules} != {expected_modules}')

version = rootpom.xpath('string(//m:properties/m:mcp-java-sdk.version)', namespaces=ns)
if version == '2.0.0':
    ok('MCP SDK version', version)
else:
    fail('MCP SDK version', version)

server_pom = (root / 'shopops-commerce-mcp-server/pom.xml').read_text()
if '<artifactId>shopops-admin</artifactId>' not in server_pom:
    ok('server module boundary', 'no shopops-admin dependency')
else:
    fail('server module boundary', 'shopops-admin dependency found')

for p in [
    root / 'shopops-admin/src/main/resources/application.yml',
    root / 'shopops-commerce-mcp-server/src/main/resources/application.yml',
]:
    try:
        with p.open(encoding='utf-8') as stream:
            yaml.safe_load(stream)
        ok(f'YAML {p.relative_to(root)}', 'parsed')
    except Exception as ex:
        fail(f'YAML {p.relative_to(root)}', str(ex))

schema = {
    '$schema': 'https://json-schema.org/draft/2020-12/schema',
    'type': 'object',
    'properties': {
        'shopId': {
            'type': 'integer',
            'minimum': 1,
            'description': 'Trusted shop scope injected by ShopOps Tool Gateway',
        },
        'startDate': {'type': 'string', 'format': 'date'},
        'endDate': {'type': 'string', 'format': 'date'},
        'minStar': {'type': 'integer', 'minimum': 1, 'maximum': 5, 'default': 3},
    },
    'required': ['shopId', 'startDate', 'endDate'],
    'additionalProperties': False,
}
canonical = json.dumps(schema, ensure_ascii=False, sort_keys=True, separators=(',', ':'))
computed_hash = hashlib.sha256(canonical.encode()).hexdigest()
sql = (root / 'shopops-admin/src/main/resources/db/migration/V22__phase8_readonly_mcp.sql').read_text()
match = re.search(r"`schema_hash` = '([0-9a-f]{64})'", sql)
if match and match.group(1) == computed_hash:
    ok('schema hash', computed_hash)
else:
    fail('schema hash', f'computed={computed_hash}, sql={match.group(1) if match else None}')

client = (root / 'shopops-admin/src/main/java/com/sirithree/shopops/admin/mcp/client/OfficialCommerceMcpClient.java').read_text()
for token in ['client.initialize()', 'client.listTools()', 'client.callTool(', 'HttpClientStreamableHttpTransport']:
    if token in client:
        ok(f'client token {token}', 'present')
    else:
        fail(f'client token {token}', 'missing')

server_config = (root / 'shopops-commerce-mcp-server/src/main/java/com/sirithree/shopops/mcp/commerce/config/McpServerConfiguration.java').read_text()
for token in ['HttpServletStreamableServerTransportProvider', 'McpServer.sync(', 'server.addTool(']:
    if token in server_config:
        ok(f'server token {token}', 'present')
    else:
        fail(f'server token {token}', 'missing')

violations = []
for p in (root / 'shopops-admin/src/main/java').rglob('*.java'):
    rel = str(p.relative_to(root))
    text = p.read_text(errors='ignore')
    references_client = (
        'import com.sirithree.shopops.admin.mcp.client.CommerceMcpClient;' in text
        or 'import com.sirithree.shopops.admin.mcp.client.OfficialCommerceMcpClient;' in text
    )
    if references_client and '/mcp/client/' not in rel and '/mcp/provider/' not in rel:
        violations.append(rel)
if violations:
    fail('MCP client routing boundary', str(violations))
else:
    ok('MCP client routing boundary', 'only client/provider packages reference MCP client')

admin_imports = []
for p in (root / 'shopops-commerce-mcp-server/src').rglob('*.java'):
    if 'com.sirithree.shopops.admin.' in p.read_text(errors='ignore'):
        admin_imports.append(str(p.relative_to(root)))
if admin_imports:
    fail('server source boundary', str(admin_imports))
else:
    ok('server source boundary', 'no admin imports')

tool_files = sorted(
    p.name
    for p in (root / 'shopops-commerce-mcp-server/src/main/java/com/sirithree/shopops/mcp/commerce/tool').glob('*McpTool.java')
)
if tool_files == ['CommentQueryNegativeMcpTool.java']:
    ok('first-batch scope', 'only comment.query_negative tool implementation')
else:
    fail('first-batch scope', str(tool_files))

result = subprocess.run(
    ['bash', '-n', str(root / 'scripts/phase8-mcp-readonly-smoke.sh')],
    capture_output=True,
    text=True,
)
if result.returncode == 0:
    ok('smoke script syntax', 'bash -n')
else:
    fail('smoke script syntax', result.stderr)

for name, status, detail in checks:
    print(f'[{status}] {name}: {detail}')
print(f'\nTOTAL={len(checks)} PASS={sum(status == "PASS" for _, status, _ in checks)} FAIL={len(errors)}')
if errors:
    sys.exit(1)
