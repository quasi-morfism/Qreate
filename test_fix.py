#!/usr/bin/env python3
"""
Test the Vue project fix
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

def test_adapt_parameter():
    """Test the new adapt parameter functionality"""
    
    # Test with existing app that has multi_file type
    app_id = 326124693326557184  # This one exists in DB with multi_file type
    
    print(f"🧪 测试adapt参数功能 - AppID: {app_id}")
    
    # Simulate frontend request with adapt=vue_project
    url = "http://localhost:8100/api/app/chat/gen/code"
    params = {
        "appId": app_id,
        "message": "Update to use Vue 3 composition API",
        "adapt": "vue_project"
    }
    
    print(f"📤 发送请求: {url}")
    print(f"📋 参数: {params}")
    
    # This would normally be an EventSource request, but we'll use regular request for testing
    # Note: This will start the generation process
    try:
        response = session.get(url, params=params, stream=True, timeout=10)
        print(f"📥 响应状态码: {response.status_code}")
        
        if response.status_code == 200:
            print("✅ 请求成功发送!")
            print("📝 检查后端日志以确认adapt参数被正确处理...")
            
            # Read first few chunks to see if it starts
            chunk_count = 0
            for chunk in response.iter_content(chunk_size=1024):
                if chunk:
                    chunk_count += 1
                    if chunk_count <= 3:  # Only show first 3 chunks
                        print(f"📦 收到数据块 {chunk_count}: {chunk[:100]}...")
                    if chunk_count >= 5:  # Stop after 5 chunks to avoid long generation
                        break
            
            print(f"📊 总共收到 {chunk_count} 个数据块")
            
        else:
            print(f"❌ 请求失败: {response.status_code}")
            print(f"❌ 响应内容: {response.text}")
            
    except requests.exceptions.Timeout:
        print("⏰ 请求超时 (这是正常的，因为代码生成需要时间)")
    except Exception as e:
        print(f"❌ 请求错误: {e}")

def check_app_after_test():
    """Check if the app's codeGenType was updated"""
    app_id = 326124693326557184
    
    url = "http://localhost:8100/api/app/get/vo"
    params = {"id": app_id}
    
    response = session.get(url, params=params)
    if response.status_code == 200:
        result = response.json()
        if result.get('code') == 0:
            app_data = result.get('data')
            print(f"\n🔍 检查应用数据更新:")
            print(f"   应用ID: {app_data.get('id')}")
            print(f"   应用名称: {app_data.get('appName')}")
            print(f"   当前代码类型: {app_data.get('codeGenType')}")
            
            if app_data.get('codeGenType') == 'vue_project':
                print("✅ 代码类型已成功更新为 vue_project!")
            else:
                print(f"⚠️ 代码类型仍然是: {app_data.get('codeGenType')}")

def main():
    if not login():
        print("❌ 登录失败")
        return
    
    print("\n" + "="*60)
    print("🔧 测试Vue项目adapt参数修复")
    print("="*60)
    
    test_adapt_parameter()
    
    # Wait a moment for any database updates
    import time
    time.sleep(2)
    
    check_app_after_test()

if __name__ == "__main__":
    main()