"""
Run the backend so the tablet can connect.
Uses host 0.0.0.0 so the server accepts connections from your LAN (e.g. 192.168.1.6).
"""
import uvicorn

if __name__ == "__main__":
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=8000,
        reload=True,
    )
