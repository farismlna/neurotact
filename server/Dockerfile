FROM python:3.11-slim

# Set working directory
WORKDIR /app

# Copy requirements dulu untuk cache layer
COPY requirements.txt .

# Install dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Copy semua file server
COPY . .

# Expose port
EXPOSE 8000

# Jalankan server
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]