"""Prepare private local verification settings without printing credentials."""

import base64
import json
import secrets
from datetime import datetime
from pathlib import Path


root = Path(__file__).resolve().parents[1]
private = root / ".local" / "payment-postman"
private.mkdir(parents=True, exist_ok=True)
private.chmod(0o700)
data_file = private / "iteration-data.json"
runtime_file = private / "runtime.properties"
run = datetime.now().strftime("%Y%m%d%H%M%S%f")

if data_file.exists():
    previous = json.loads(data_file.read_text())[0]
    admin_email = previous["admin_email"]
    admin_password = previous["admin_password"]
elif runtime_file.exists():
    # Recover this tool's generated login settings, never the application's
    # existing application-local.properties or its database credentials.
    values = dict(line.split("=", 1) for line in runtime_file.read_text().splitlines() if "=" in line)
    admin_email = values["INITIAL_ADMIN_EMAIL"]
    admin_password = values["INITIAL_ADMIN_PASSWORD"]
else:
    admin_email = "jms.payment.verify." + run + "@example.test"
    admin_password = secrets.token_urlsafe(24)

if not runtime_file.exists():
    properties = {
        "spring.datasource.url": "jdbc:mysql://127.0.0.1:3306/Jewellery_Shop",
        "spring.jpa.hibernate.ddl-auto": "update",
        "security.jwt.secret": base64.b64encode(secrets.token_bytes(64)).decode(),
        "security.jwt.expiration-ms": "3600000",
        "app.cors.allowed-origins": "http://localhost:3000",
        "INITIAL_ADMIN_NAME": "JMS Payment Verification Admin",
        "INITIAL_ADMIN_EMAIL": admin_email,
        "INITIAL_ADMIN_PASSWORD": admin_password,
    }
    runtime_file.touch(mode=0o600)
    runtime_file.write_text("\n".join(key + "=" + value for key, value in properties.items()) + "\n")

data = {
    "admin_email": admin_email,
    "admin_password": admin_password,
    "staff_email": "jms.sales.verify." + run + "@example.test",
    "staff_password": secrets.token_urlsafe(24),
    "run_id": run,
    "customer_phone": "9" + "".join(str(secrets.randbelow(10)) for _ in range(9)),
}
data_file.touch(mode=0o600)
data_file.write_text(json.dumps([data], indent=2) + "\n")
for path in (data_file, runtime_file):
    path.chmod(0o600)
print("Private verification settings ready. Credentials are not printed.")
print("A new run creates labelled records; it does not delete previous records.")
