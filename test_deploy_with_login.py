#!/usr/bin/env python3
"""
Test deployment functionality with real user login
"""
import requests
import json

session = requests.Session()

def login():
    """Login with provided credentials"""
    url = "http://localhost:8100/api/user/login"
    data = {
        "userAccount": "qu100",
        "userPassword": "12345678"
    }
    
    print("🔐 正在登录...")
    print(f"用户名: {data['userAccount']}")
    
    response = session.post(url, json=data)
    print(f"登录状态码: {response.status_code}")
    print(f"登录响应: {response.text}")
    
    if response.status_code == 200:
        result = response.json()
        if result.get('code') == 0:
            print("✅ 登录成功!")
            return True
        else:
            print(f"❌ 登录失败: {result.get('message')}")
            return False
    else:
        print(f"❌ 登录请求失败: {response.status_code}")
        return False

def get_user_apps():
    """Get user's applications"""
    url = "http://localhost:8100/api/app/my/list/page/vo"
    data = {
        "current": 1,
        "pageSize": 20
    }
    
    print("\n📱 获取用户应用列表...")
    response = session.post(url, json=data)
    print(f"状态码: {response.status_code}")
    
    if response.status_code == 200:
        result = response.json()
        if result.get('code') == 0:
            apps = result.get('data', {}).get('records', [])
            print(f"✅ 找到 {len(apps)} 个应用:")
            for app in apps:
                print(f"  - ID: {app.get('id')}, 名称: {app.get('appName')}, 类型: {app.get('codeGenType')}")
            return apps
        else:
            print(f"❌ 获取应用失败: {result.get('message')}")
    else:
        print(f"❌ 请求失败: {response.status_code}")
    return []

def test_deploy(app_id):
    """Test deployment for a specific app"""
    url = "http://localhost:8100/api/app/deploy"
    data = {"appId": app_id}
    
    print(f"\n🚀 测试部署应用 {app_id}...")
    response = session.post(url, json=data)
    print(f"状态码: {response.status_code}")
    print(f"响应: {response.text}")
    
    if response.status_code == 200:
        result = response.json()
        print(f"响应代码: {result.get('code')}")
        print(f"消息: {result.get('message')}")
        print(f"数据: {result.get('data')}")
        
        if result.get('code') == 0:
            print("✅ 部署成功!")
            deploy_url = result.get('data')
            if deploy_url:
                print(f"🌐 部署URL: {deploy_url}")
        else:
            print(f"❌ 部署失败: {result.get('message')}")
    else:
        print(f"❌ 部署请求失败: {response.status_code}")

def main():
    # Step 1: Login
    if not login():
        return
    
    # Step 2: Get user apps
    apps = get_user_apps()
    if not apps:
        print("❌ 没有找到任何应用，无法测试部署")
        return
    
    # Step 3: Test deployment for each app
    for app in apps:
        app_id = app.get('id')
        app_name = app.get('appName')
        code_type = app.get('codeGenType')
        
        print(f"\n{'='*50}")
        print(f"测试应用: {app_name} (ID: {app_id}, 类型: {code_type})")
        print('='*50)
        
        test_deploy(app_id)

if __name__ == "__main__":
    main()