import pytest
import json
from app import app, db, User
from flask_jwt_extended import create_access_token


@pytest.fixture
def client():
    app.config['TESTING'] = True
    app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///test.db'

    with app.test_client() as client:
        with app.app_context():
            db.create_all()
        yield client
        with app.app_context():
            db.drop_all()


class TestScenario1Registration:

    def test_successful_registration(self, client):
        """Тест успешной регистрации пользователя"""
        data = {
            "email": "newuser@example.com",
            "password": "password123",
            "first_name": "Иван",
            "last_name": "Петров"
        }

        response = client.post('/api/register',
                               data=json.dumps(data),
                               content_type='application/json')

        assert response.status_code == 201
        json_data = response.get_json()
        assert json_data['message'] == "Пользователь создан успешно!"
        assert 'access_token' in json_data
        assert json_data['next_step'] == "complete_profile"

        # Проверяем, что пользователь создан в базе
        with app.app_context():
            user = User.query.filter_by(email="newuser@example.com").first()
            assert user is not None
            assert user.first_name == "Иван"
            assert user.last_name == "Петров"
            assert user.profile_completed == False

    def test_registration_existing_email(self, client):
        """Тест регистрации с существующим email"""
        # Сначала создаем пользователя
        user = User(
            email="existing@example.com",
            first_name="Test",
            last_name="User"
        )
        user.set_password("password123")

        with app.app_context():
            db.session.add(user)
            db.session.commit()

        # Пытаемся создать пользователя с тем же email
        data = {
            "email": "existing@example.com",
            "password": "password456",
            "first_name": "Другой",
            "last_name": "Пользователь"
        }

        response = client.post('/api/register',
                               data=json.dumps(data),
                               content_type='application/json')

        assert response.status_code == 400
        json_data = response.get_json()
        assert "уже существует" in json_data['error']

    def test_registration_missing_fields(self, client):
        """Тест регистрации с отсутствующими полями"""
        data = {
            "email": "test@example.com",
            # password отсутствует
            "first_name": "Иван"
            # last_name отсутствует
        }

        response = client.post('/api/register',
                               data=json.dumps(data),
                               content_type='application/json')

        assert response.status_code == 400
        json_data = response.get_json()
        assert "обязательные поля" in json_data['error'].lower()

    def test_registration_invalid_email(self, client):
        """Тест регистрации с невалидным email"""
        data = {
            "email": "invalid-email",
            "password": "password123",
            "first_name": "Иван",
            "last_name": "Петров"
        }

        response = client.post('/api/register',
                               data=json.dumps(data),
                               content_type='application/json')

        assert response.status_code == 400
        json_data = response.get_json()
        assert "формат email" in json_data['error'].lower()

    def test_registration_short_password(self, client):
        """Тест регистрации с коротким паролем"""
        data = {
            "email": "test@example.com",
            "password": "123",  # слишком короткий
            "first_name": "Иван",
            "last_name": "Петров"
        }

        response = client.post('/api/register',
                               data=json.dumps(data),
                               content_type='application/json')

        assert response.status_code == 400
        json_data = response.get_json()
        assert "6 символов" in json_data['error']