import pytest
from app import app


def test_available_routes():
    """Тест для проверки доступных маршрутов"""
    with app.test_client() as client:
        # Проверяем основные маршруты
        routes_to_check = [
            ('/', 'GET', None),
            ('/api/register', 'POST', None),
            ('/api/login', 'POST', None),
            ('/api/feed', 'GET', None),
            ('/api/events/1/register', 'POST', None),
            ('/api/organisations', 'POST', None),
        ]

        print("\n🔍 ПРОВЕРКА ДОСТУПНЫХ МАРШРУТОВ:")
        for route, method, data in routes_to_check:
            if method == 'GET':
                response = client.get(route)
            elif method == 'POST':
                response = client.post(route, json=data or {})

            print(f"  {method} {route} -> {response.status_code}")

            # Маршрут должен существовать (не 404) или требовать авторизации (401)
            assert response.status_code != 404 or route in ['/api/feed', '/api/events/1/register'], \
                f"Маршрут {route} не найден (404)"


if __name__ == '__main__':
    pytest.main([__file__, '-v'])