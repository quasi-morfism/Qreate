#!/usr/bin/env python3
"""
Test Vue project deployment with direct IDs from filesystem
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

def main():
    if not login():
        print("❌ 登录失败")
        return
    
    # Vue project IDs from filesystem directories
    vue_app_ids = [
        1,  # vue_project_1
        326124693326557184,  # vue_project_326124693326557184
        326394947705458688,  # vue_project_326394947705458688
        326400125011709952,  # vue_project_326400125011709952
        326402496173395968,  # vue_project_326402496173395968
        326402925988892672,  # vue_project_326402925988892672
        326403535287046144,  # vue_project_326403535287046144
        326404132828565504,  # vue_project_326404132828565504 (the one with dist)
        326404808065372160,  # vue_project_326404808065372160
    ]
    
    print(f"📁 从文件系统找到的Vue项目IDs: {len(vue_app_ids)} 个")
    
    for app_id in vue_app_ids:
        print(f"\n{'='*60}")
        print(f"测试Vue项目ID: {app_id}")
        print(f"对应目录: vue_project_{app_id}")
        print('='*60)
        
        deploy_url = test_vue_deploy(app_id)

if __name__ == "__main__":
    main()