# test_register.py
import requests
import json


def test_register():
    url = "http://localhost:5000/api/register"

    data = {
        "email": "test@example.com",
        "password": "123456",
        "first_name": "Анна",
        "last_name": "Тестова"
    }

    try:
        response = requests.post(url, json=data)
        print("Статус код:", response.status_code)
        print("Ответ:", response.json())
    except Exception as e:
        print("Ошибка запроса:", e)


def test_login():
    url = "http://localhost:5000/api/login"

    data = {
        "email": "test@example.com",
        "password": "123456"
    }

    try:
        response = requests.post(url, json=data)
        print("Статус код:", response.status_code)
        print("Ответ:", response.json())
    except Exception as e:
        print("Ошибка запроса:", e)


if __name__ == "__main__":
    print("🔐 Тестируем регистрацию...")
    test_register()

    print("\n🔐 Тестируем вход...")
    test_login()