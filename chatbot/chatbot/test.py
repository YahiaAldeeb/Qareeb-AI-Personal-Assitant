import requests

url = "http://localhost:8000/transcribe"
files = {"file": open("your_audio.wav", "rb")}
response = requests.post(url, files=files)

print(response.json())
