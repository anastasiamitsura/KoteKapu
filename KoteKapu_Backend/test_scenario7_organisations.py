import pytest
import json
from app import app, db, User, Organisation
from flask_jwt_extended import create_access_token


@pytest.fixture
def client():
    app.config['TESTING'] = True
    app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///test.db'

    with app.test_client() as client:
        with app.app_context():
            db.create_all()
            setup_test_org_data()
        yield client
        with app.app_context():
            db.drop_all()


def setup_test_org_data():
    """Создает тестовые данные для организаций"""
    user = User(
        email="test@example.com",
        first_name="Test",
        last_name="User",
        profile_completed=True,
        preferences_completed=True
    )
    user.set_password("password123")
    db.session.add(user)

    # Создаем тестовую организацию
    org = Organisation(
        title="Existing Org",
        description="Existing Organization",
        owner_id=1,
        status="approved",
        city="Москва"
    )
    org.tags = json.dumps(["IT", "технологии"])
    db.session.add(org)
    db.session.commit()


@pytest.fixture
def auth_headers():
    with app.app_context():
        access_token = create_access_token(identity=1)
        return {'Authorization': f'Bearer {access_token}'}


class TestScenario7Organisations:

    def test_create_organisation_success(self, client, auth_headers):
        """Тест успешного создания организации"""
        data = {
            "title": "Новая организация",
            "description": "Описание новой организации",
            "city": "Москва",
            "tags": ["образование", "наука"],
            "social_links": ["https://example.com"]
        }

        response = client.post('/api/organisations',
                               data=json.dumps(data),
                               content_type='application/json',
                               headers=auth_headers)

        assert response.status_code == 201
        json_data = response.get_json()
        assert "создана и отправлена на модерацию" in json_data['message']
        assert json_data['organisation']['title'] == "Новая организация"
        assert json_data['organisation']['status'] == "pending"

    def test_create_organisation_duplicate_title(self, client, auth_headers):
        """Тест создания организации с существующим названием"""
        data = {
            "title": "Existing Org",  # Такое же название как у существующей
            "description": "Другое описание"
        }

        response = client.post('/api/organisations',
                               data=json.dumps(data),
                               content_type='application/json',
                               headers=auth_headers)

        assert response.status_code == 400
        json_data = response.get_json()
        assert "уже существует" in json_data['error']

    def test_create_organisation_missing_fields(self, client, auth_headers):
        """Тест создания организации без обязательных полей"""
        data = {
            # title отсутствует
            "description": "Описание"
        }

        response = client.post('/api/organisations',
                               data=json.dumps(data),
                               content_type='application/json',
                               headers=auth_headers)

        assert response.status_code == 400
        json_data = response.get_json()
        assert "обязательны" in json_data['error']

    def test_get_organisation_details(self, client):
        """Тест получения информации об организации"""
        response = client.get('/api/organisations/1')

        assert response.status_code == 200
        json_data = response.get_json()

        org = json_data['organisation']
        assert org['id'] == 1
        assert org['title'] == "Existing Org"
        assert org['description'] == "Existing Organization"
        assert org['status'] == "approved"
        assert 'events_count' in org
        assert 'subscribers_count' in org
        assert 'tags' in org

    def test_subscribe_to_organisation(self, client, auth_headers):
        """Тест подписки на организацию"""
        response = client.post('/api/organisations/1/subscribe',
                               headers=auth_headers)

        assert response.status_code == 200
        json_data = response.get_json()
        assert "успешно подписались" in json_data['message']
        assert 'subscribers_count' in json_data

        # Проверяем что пользователь добавлен в подписчики
        with app.app_context():
            org = Organisation.query.get(1)
            user = User.query.get(1)
            assert user in org.subscribers

    def test_subscribe_twice(self, client, auth_headers):
        """Тест повторной подписки на организацию"""
        # Первая подписка
        client.post('/api/organisations/1/subscribe', headers=auth_headers)

        # Вторая подписка
        response = client.post('/api/organisations/1/subscribe',
                               headers=auth_headers)

        assert response.status_code == 400
        json_data = response.get_json()
        assert "уже подписаны" in json_data['error']

    def test_subscribe_nonexistent_organisation(self, client, auth_headers):
        """Тест подписки на несуществующую организацию"""
        response = client.post('/api/organisations/999/subscribe',
                               headers=auth_headers)

        assert response.status_code == 404

    def test_organisation_creation_flow(self, client, auth_headers):
        """Тест полного цикла создания организации"""
        # Создаем организацию
        create_data = {
            "title": "Test Flow Org",
            "description": "Organization for testing flow",
            "city": "Санкт-Петербург",
            "tags": ["тестирование", "разработка"]
        }

        create_response = client.post('/api/organisations',
                                      data=json.dumps(create_data),
                                      content_type='application/json',
                                      headers=auth_headers)

        assert create_response.status_code == 201
        org_id = create_response.get_json()['organisation']['id']

        # Подписываемся на организацию
        subscribe_response = client.post(f'/api/organisations/{org_id}/subscribe',
                                         headers=auth_headers)

        assert subscribe_response.status_code == 200

        # Получаем информацию об организации
        details_response = client.get(f'/api/organisations/{org_id}')

        assert details_response.status_code == 200
        org_details = details_response.get_json()['organisation']
        assert org_details['title'] == "Test Flow Org"
        assert org_details['subscribers_count'] == 1