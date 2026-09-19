# main.py

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List
from gemini_handler import process_speech
from morse_mapper import TACTICAL_MAPPING
import time

app = FastAPI(title="NeuroTact STT Server")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

class SpeechInput(BaseModel):
    text: str

class TacticalItem(BaseModel):
    code      : str
    label     : str
    timestamp : int    # Unix timestamp dalam milidetik
    delay_ms  : int    # Berapa milidetik setelah item pertama harus dikirim

class ProcessResponse(BaseModel):
    results : List[TacticalItem]
    status  : str
    message : str

@app.get("/")
def root():
    return {"message": "NeuroTact STT Server aktif"}

@app.get("/mapping")
def get_mapping():
    return TACTICAL_MAPPING

@app.get("/health")
def health():
    return {"status": "ok"}

@app.post("/process", response_model=ProcessResponse)
async def process(input: SpeechInput):
    if not input.text or len(input.text.strip()) == 0:
        raise HTTPException(status_code=400, detail="Teks tidak boleh kosong")

    result      = process_speech(input.text)
    base_time   = int(time.time() * 1000)  # Unix timestamp milidetik
    delay_per_item = 2000                  # 2 detik antar item

    items = []
    for i, item in enumerate(result["results"]):
        items.append(TacticalItem(
            code      = item["code"],
            label     = item["label"],
            timestamp = base_time + (i * delay_per_item),
            delay_ms  = i * delay_per_item
        ))

    return ProcessResponse(
        results = items,
        status  = result["status"],
        message = result["message"]
    )