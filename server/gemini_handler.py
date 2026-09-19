# gemini_handler.py

import json
import os
from dotenv import load_dotenv
from google import genai
from morse_mapper import get_mapping_description, TACTICAL_MAPPING

load_dotenv()

client = genai.Client(api_key=os.getenv("GEMINI_API_KEY"))

def build_prompt(text: str) -> str:
    mapping = get_mapping_description()
    return f"""Kamu adalah sistem konversi instruksi taktis sepakbola untuk atlet tunagrahita.

Diberikan teks instruksi dari pelatih, tentukan instruksi taktis utama yang dimaksud
dan kembalikan kode huruf yang sesuai berdasarkan mapping berikut:

{mapping}

Aturan penting:
- Kembalikan HANYA satu huruf kode yang paling relevan
- Jika tidak ada yang cocok, kembalikan huruf E (Tahan Posisi) sebagai default
- Kembalikan HANYA JSON tanpa teks tambahan, tanpa markdown, tanpa backtick

Input pelatih: "{text}"

Format response yang WAJIB diikuti:
{{"code": "[huruf]"}}"""

def process_speech(text: str) -> dict:
    try:
        prompt = build_prompt(text)

        response = client.models.generate_content(
            model="gemini-3.6-flash",
            contents=prompt
        )

        raw = response.text.strip()

        # Bersihkan jika ada backtick atau markdown
        raw = raw.replace("```json", "").replace("```", "").strip()

        result = json.loads(raw)

        # Validasi format
        if "code" not in result:
            raise ValueError("Key 'code' tidak ditemukan")

        code = result["code"].upper()

        # Validasi kode valid
        if code not in TACTICAL_MAPPING:
            code = "E"

        return {"code": code, "status": "ok", "message": ""}

    except json.JSONDecodeError:
        return {
            "code": "E",
            "status": "error",
            "message": "Gemini return format tidak valid"
        }

    except Exception as e:
        return {
            "code": "E",
            "status": "error",
            "message": str(e)
        }