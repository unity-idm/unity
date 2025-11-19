#!/usr/bin/python3
import sys, json

try:
    data = json.load(sys.stdin)         
    result = {
            "status": "PROCEED",
            "claims": [ {
                "name": "test",
                "values": ['test']
            }, {
                "name": "test2",
                "values": [1,2]
            }  ]
    }
    json.dump(result, sys.stdout)  
    sys.stdout.flush()             
except Exception as e:
    result = {
        "status": "DENY"
    }
    json.dump(result, sys.stdout)  
    sys.stdout.flush()
