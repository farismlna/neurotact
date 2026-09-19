# main.py

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from gemini_handler import process_speech
from morse_mapper import get_morse, TACTICAL_MAPPING

app = FastAPI(title="NeuroTact STT Server")

# Izinkan request dari Android dan semua origin lokal
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

class SpeechInput(BaseModel):
    text: str

class TacticalResponse(BaseModel):
    code: str
    morse: str
    label: str
    status: str
    message: str = ""

@app.get("/")
def root():
    return {"message": "NeuroTact STT Server aktif"}

@app.get("/mapping")
def get_mapping():
    """Endpoint untuk melihat semua mapping instruksi yang tersedia"""
    return TACTICAL_MAPPING

@app.post("/process", response_model=TacticalResponse)
async def process(input: SpeechInput):
    if not input.text or len(input.text.strip()) == 0:
        raise HTTPException(status_code=400, detail="Teks tidak boleh kosong")
    
    result = process_speech(input.text)
    code = result["code"]
    
    return {
        "code": code,
        "morse": get_morse(code),
        "label": TACTICAL_MAPPING[code]["label"],
        "status": result["status"],
        "message": result.get("message", "")
    }

@app.get("/health")
def health():
    return {"status": "ok"}