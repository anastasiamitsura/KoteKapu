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


@pytest.fixture
def auth_headers():
    """Создает тестового пользователя и возвращает заголовки авторизации"""
    with app.app_context():
        user = User(
            email="test@example.com",
            first_name="Test",
            last_name="User"
        )
        user.set_password("password123")
        db.session.add(user)
        db.session.commit()

        access_token = create_access_token(identity=user.id)
        return {'Authorization': f'Bearer {access_token}'}


class TestScenario2Profile:

    def test_complete_profile_success(self, client, auth_headers):
        """Тест успешного заполнения профиля"""
        data = {
            "phone": "+79991234567",
            "age_user": 25,
            "placement": "Москва",
            "study_place": "МГУ",
            "grade_course": "3 курс"
        }

        response = client.post('/api/users/1/complete-profile',
                               data=json.dumps(data),
                               content_type='application/json',
                               headers=auth_headers)

        assert response.status_code == 200
        json_data = response.get_json()
        assert json_data['message'] == "Профиль успешно обновлен"
        assert json_data['next_step'] == "complete_preferences"

        # Проверяем обновление данных в базе
        with app.app_context():
            user = User.query.get(1)
            assert user.phone == "+79991234567"
            assert user.age_user == 25
            assert user.placement == "Москва"
            assert user.study_place == "МГУ"
            assert user.grade_course == "3 курс"
            assert user.profile_completed == True

    def test_complete_profile_unauthorized(self, client):
        """Тест заполнения профиля без авторизации"""
        data = {
            "phone": "+79991234567",
            "age_user": 25
        }

        response = client.post('/api/users/1/complete-profile',
                               data=json.dumps(data),
                               content_type='application/json')

        assert response.status_code == 401  # Unauthorized

    def test_complete_profile_wrong_user(self, client, auth_headers):
        """Тест заполнения профиля другого пользователя"""
        data = {
            "phone": "+79991234567",
            "age_user": 25
        }

        response = client.post('/api/users/2/complete-profile',  # Чужой ID
                               data=json.dumps(data),
                               content_type='application/json',
                               headers=auth_headers)

        assert response.status_code == 403  # Forbidden

    def test_complete_profile_partial_data(self, client, auth_headers):
        """Тест заполнения профиля частичными данными"""
        data = {
            "phone": "+79991234567",
            "age_user": 25
            # Остальные поля отсутствуют
        }

        response = client.post('/api/users/1/complete-profile',
                               data=json.dumps(data),
                               content_type='application/json',
                               headers=auth_headers)

        assert response.status_code == 200
        json_data = response.get_json()

        with app.app_context():
            user = User.query.get(1)
            assert user.phone == "+79991234567"
            assert user.age_user == 25
            assert user.profile_completed == True

    def test_get_preference_categories(self, client):
        """Тест получения категорий для опроса предпочтений"""
        response = client.get('/api/preferences/categories')

        assert response.status_code == 200
        json_data = response.get_json()

        assert 'interest_categories' in json_data
        assert 'format_types' in json_data
        assert 'event_types' in json_data
        assert len(json_data['interest_categories']) > 0
        assert len(json_data['format_types']) > 0
        assert len(json_data['event_types']) > 0