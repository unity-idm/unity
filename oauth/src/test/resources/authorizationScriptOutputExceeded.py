#!/usr/bin/python3
import sys, json

#!/usr/bin/env python3
import sys
import json

try:
    data = json.load(sys.stdin)
    BIG_VALUE_SIZE = 1024 * 1024 + 100  # > 1 MB
    big_string = "A" * BIG_VALUE_SIZE

    result = {
        "status": "PROCEED",
        "claims": [
            {
                "name": "test",
                "values": ["test"]
            },
            {
                "name": "test2",
                "values": [1, 2]
            },
            {
                "name": "big_claim",
                "values": [big_string]
            }
        ]
    }

    json.dump(result, sys.stdout)
    sys.stdout.flush()

except Exception:
    result = {
        "status": "DENY"
    }
    json.dump(result, sys.stdout)
    sys.stdout.flush()
