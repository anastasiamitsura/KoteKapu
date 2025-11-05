# Use official Python slim image
FROM python:3.11-slim

# set workdir
WORKDIR /app

# install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# copy app
COPY . .

ENV FLASK_APP=manage.py
ENV FLASK_ENV=production

# create instance folder for sqlite if needed
RUN mkdir -p /app/instance

CMD ["gunicorn", "-b", "0.0.0.0:8000", "manage:app"]
