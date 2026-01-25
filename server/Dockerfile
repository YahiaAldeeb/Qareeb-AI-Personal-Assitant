# Optimized Dockerfile with layer caching
# Build with: DOCKER_BUILDKIT=1 docker build -t qareeb-server .
# Or enable BuildKit globally: export DOCKER_BUILDKIT=1

FROM python:3.10-slim

WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    ffmpeg \
    libsndfile1 \
    && rm -rf /var/lib/apt/lists/*

# Upgrade pip first (cached layer)
RUN pip install --upgrade pip

# Install base dependencies (rarely change - best caching)
COPY requirements-base.txt .
RUN --mount=type=cache,target=/root/.cache/pip \
    pip install -r requirements-base.txt

# Install PyTorch (large, stable - separate layer for better caching)
RUN --mount=type=cache,target=/root/.cache/pip \
    pip install torch==2.0.1 torchaudio==2.0.2 --index-url https://download.pytorch.org/whl/cpu

# Install audio dependencies (stable)
COPY requirements-audio.txt .
RUN --mount=type=cache,target=/root/.cache/pip \
    pip install  -r requirements-audio.txt

# Install LLM dependencies (most likely to change - install last)
COPY requirements-llm.txt .
RUN --mount=type=cache,target=/root/.cache/pip \
    pip install -r requirements-llm.txt

# Copy application code (changes frequently, but small)
COPY . .

# Note: Environment variables should be passed at runtime:
# docker run --env-file .env -p 8000:8000 qareeb-server


CMD ["python", "command_server.py"]
