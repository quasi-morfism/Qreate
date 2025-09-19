#!/usr/bin/env python3
"""
Test Vue project deployment specifically
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
    
    response = session.post(url, json=data)
    if response.status_code == 200:
        result = response.json()
        if result.get('code') == 0:
            print("✅ 登录成功!")
            return True
    return False

def get_all_apps():
    """Get all applications to find Vue projects"""
    url = "http://localhost:8100/api/app/admin/list/page/vo"
    data = {
        "current": 1,
        "pageSize": 100  # Get more apps to find Vue projects
    }
    
    print("\n📱 获取所有应用列表...")
    response = session.post(url, json=data)
    
    if response.status_code == 200:
        result = response.json()
        if result.get('code') == 0:
            apps = result.get('data', {}).get('records', [])
            print(f"✅ 总共找到 {len(apps)} 个应用")
            
            vue_apps = [app for app in apps if app.get('codeGenType') == 'vue_project']
            print(f"✅ 找到 {len(vue_apps)} 个Vue项目:")
            for app in vue_apps:
                print(f"  - ID: {app.get('id')}, 名称: {app.get('appName')}")
            
            return vue_apps
    return []

def test_vue_deploy(app_id):
    """Test deployment for a Vue project"""
    url = "http://localhost:8100/api/app/deploy"
    data = {"appId": app_id}
    
    print(f"\n🚀 测试Vue项目部署 {app_id}...")
    response = session.post(url, json=data)
    print(f"状态码: {response.status_code}")
    print(f"响应: {response.text}")
    
    if response.status_code == 200:
        result = response.json()
        print(f"响应代码: {result.get('code')}")
        print(f"消息: {result.get('message')}")
        print(f"数据: {result.get('data')}")
        
        if result.get('code') == 0:
            print("✅ Vue项目部署成功!")
            deploy_url = result.get('data')
            if deploy_url:
                print(f"🌐 部署URL: {deploy_url}")
                return deploy_url
        else:
            print(f"❌ Vue项目部署失败: {result.get('message')}")
    else:
        print(f"❌ 部署请求失败: {response.status_code}")
    return None

def test_deployed_app(deploy_url):
    """Test if deployed app is accessible"""
    if not deploy_url:
        return
        
    print(f"\n🌐 测试部署的应用访问: {deploy_url}")
    try:
        response = requests.get(deploy_url, timeout=10)
        print(f"访问状态码: {response.status_code}")
        if response.status_code == 200:
            print("✅ 部署的应用可以正常访问!")
            content_length = len(response.text)
            print(f"📄 响应内容长度: {content_length} 字符")
            if "<!DOCTYPE html>" in response.text or "<html" in response.text:
                print("✅ 返回的是HTML内容，部署成功!")
            else:
                print("⚠️ 返回的不是HTML内容")
        else:
            print(f"❌ 部署的应用访问失败: {response.status_code}")
    except Exception as e:
        print(f"❌ 访问部署应用时出错: {e}")

def main():
    if not login():
        print("❌ 登录失败")
        return
    
    # Get Vue projects
    vue_apps = get_all_apps()
    if not vue_apps:
        print("❌ 没有找到Vue项目")
        return
    
    # Test deployment for each Vue project
    for app in vue_apps[:3]:  # Test first 3 Vue apps
        app_id = app.get('id')
        app_name = app.get('appName')
        
        print(f"\n{'='*60}")
        print(f"测试Vue项目: {app_name} (ID: {app_id})")
        print('='*60)
        
        deploy_url = test_vue_deploy(app_id)
        if deploy_url:
            test_deployed_app(deploy_url)

if __name__ == "__main__":
    main()