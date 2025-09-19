#!/usr/bin/env python3
"""
Test deployment functionality by calling the API directly
"""
import requests
import json

# Test deployment
def test_deploy():
    url = "http://localhost:8100/api/app/deploy"
    headers = {
        "Content-Type": "application/json"
    }
    
    # Test with appId that exists in the code_output directory
    # vue_project_326404132828565504 exists
    data = {
        "appId": 326404132828565504
    }
    
    print(f"Testing deployment for appId: {data['appId']}")
    print(f"Expected directory: vue_project_{data['appId']}")
    
    try:
        response = requests.post(url, headers=headers, json=data)
        print(f"Status Code: {response.status_code}")
        print(f"Response: {response.text}")
        
        if response.status_code == 200:
            result = response.json()
            print(f"Response Code: {result.get('code')}")
            print(f"Message: {result.get('message')}")
            print(f"Data: {result.get('data')}")
        
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    test_deploy()