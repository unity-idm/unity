#!/usr/bin/python3

import sys
import json
def main():
    try:
        input_data = json.load(sys.stdin)
        
        # Example checks:
        # Deny if any attribute named 'blocked' is present
        for attr in input_data.get("attributes", []):
            if attr.get("name") == "blocked":
                response = {
                    "status": "Deny",
                    "claims": []
                }
                json.dump(response, sys.stdout)
                sys.stdout.flush()
                return
        
        # Deny if any identity with typeId 'banned' is present
        for ident in input_data.get("identities", []):
            if ident.get("typeId") == "banned":
                response = {
                    "status": "Deny",
                    "claims": []
                }
                json.dump(response, sys.stdout)
                sys.stdout.flush()
                return
        
        # Deny if request clientID is 'evil-client'
        request = input_data.get("request", {})
        if request.get("clientID") == "evil-client":
            response = {
                "status": "Deny",
                "claims": []
            }
            json.dump(response, sys.stdout)
            sys.stdout.flush()
            return
        
        # Otherwise, proceed and add a claim
        response = {
            "status": "Proceed"
        }
        json.dump(response, sys.stdout)
        sys.stdout.flush()
    except Exception:
        response = {
            "status": "Deny",
            "claims": []
        }
        json.dump(response, sys.stdout)
        sys.stdout.flush()
if __name__ == "__main__":
    main()