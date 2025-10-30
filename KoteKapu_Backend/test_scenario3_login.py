import pytest
import json
from app import app, db, User


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


@pytest.fixture
def test_user():
    """Создает тестового пользователя"""
    with app.app_context():
        user = User(
            email="test@example.com",
            first_name="Test",
            last_name="User",
            phone="+79991234567",
            age_user=25,
            placement="Москва",
            study_place="МГУ",
            profile_completed=True,
            preferences_completed=True
        )
        user.set_password("password123")
        db.session.add(user)
        db.session.commit()
        return user


class TestScenario3Login:

    def test_successful_login(self, client, test_user):
        """Тест успешного входа в систему"""
        data = {
            "email": "test@example.com",
            "password": "password123"
        }

        response = client.post('/api/login',
                               data=json.dumps(data),
                               content_type='application/json')

        assert response.status_code == 200
        json_data = response.get_json()
        assert json_data['message'] == "Вход выполнен успешно!"
        assert 'access_token' in json_data
        assert 'user' in json_data
        assert json_data['user']['email'] == "test@example.com"
        assert json_data['next_step'] == "main"

    def test_login_wrong_password(self, client, test_user):
        """Тест входа с неверным паролем"""
        data = {
            "email": "test@example.com",
            "password": "wrongpassword"
        }

        response = client.post('/api/login',
                               data=json.dumps(data),
                               content_type='application/json')

        assert response.status_code == 401
        json_data = response.get_json()
        assert "неверный email или пароль" in json_data['error'].lower()

    def test_login_nonexistent_user(self, client):
        """Тест входа несуществующего пользователя"""
        data = {
            "email": "nonexistent@example.com",
            "password": "password123"
        }

        response = client.post('/api/login',
                               data=json.dumps(data),
                               content_type='application/json')

        assert response.status_code == 401
        json_data = response.get_json()
        assert "неверный email или пароль" in json_data['error'].lower()

    def test_login_missing_credentials(self, client):
        """Тест входа без учетных данных"""
        data = {
            # email отсутствует
            "password": "password123"
        }

        response = client.post('/api/login',
                               data=json.dumps(data),
                               content_type='application/json')

        assert response.status_code == 400
        json_data = response.get_json()
        assert "обязательны" in json_data['error'].lower()

    def test_login_incomplete_profile_flow(self, client):
        """Тест входа пользователя с незавершенным профилем"""
        with app.app_context():
            user = User(
                email="incomplete@example.com",
                first_name="Incomplete",
                last_name="User",
                profile_completed=False  # Профиль не завершен
            )
            user.set_password("password123")
            db.session.add(user)
            db.session.commit()

        data = {
            "email": "incomplete@example.com",
            "password": "password123"
        }

        response = client.post('/api/login',
                               data=json.dumps(data),
                               content_type='application/json')

        assert response.status_code == 200
        json_data = response.get_json()
        assert json_data['next_step'] == "complete_profile"

    def test_login_incomplete_preferences_flow(self, client):
        """Тест входа пользователя с незавершенными предпочтениями"""
        with app.app_context():
            user = User(
                email="noprefs@example.com",
                first_name="NoPrefs",
                last_name="User",
                profile_completed=True,
                preferences_completed=False  # Предпочтения не завершены
            )
            user.set_password("password123")
            db.session.add(user)
            db.session.commit()

        data = {
            "email": "noprefs@example.com",
            "password": "password123"
        }

        response = client.post('/api/login',
                               data=json.dumps(data),
                               content_type='application/json')

        assert response.status_code == 200
        json_data = response.get_json()
        assert json_data['next_step'] == "complete_preferences"