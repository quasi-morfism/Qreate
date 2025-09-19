#!/usr/bin/env python3
"""
Check Vue apps database records vs filesystem
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

def get_app_details(app_id):
    """Get app details by ID"""
    url = "http://localhost:8100/api/app/get/vo"
    params = {"id": app_id}
    
    response = session.get(url, params=params)
    if response.status_code == 200:
        result = response.json()
        if result.get('code') == 0:
            return result.get('data')
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
    
    print("🔍 检查Vue项目目录对应的数据库记录:")
    print("="*80)
    
    for app_id in vue_app_ids:
        print(f"\n📁 vue_project_{app_id}")
        print("-" * 40)
        
        app_details = get_app_details(app_id)
        if app_details:
            print(f"✅ 数据库记录存在:")
            print(f"   ID: {app_details.get('id')}")
            print(f"   名称: {app_details.get('appName')}")
            print(f"   代码类型: {app_details.get('codeGenType')}")
            print(f"   用户ID: {app_details.get('userId')}")
            print(f"   创建时间: {app_details.get('createTime')}")
            print(f"   部署Key: {app_details.get('deployKey', 'None')}")
            
            # Check if codeGenType matches expected vue_project
            if app_details.get('codeGenType') != 'vue_project':
                print(f"❌ 代码类型不匹配! 期望: vue_project, 实际: {app_details.get('codeGenType')}")
            else:
                print(f"✅ 代码类型正确: {app_details.get('codeGenType')}")
        else:
            print("❌ 数据库中没有找到对应记录")

if __name__ == "__main__":
    main()