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

Diberikan teks instruksi dari pelatih, identifikasi SEMUA instruksi taktis yang ada
dan kembalikan kode huruf yang sesuai untuk SETIAP instruksi berdasarkan mapping berikut:

{mapping}

Aturan penting:
- Jika ada SATU instruksi, return array dengan satu elemen
- Jika ada DUA atau lebih instruksi berbeda, return array dengan elemen sesuai urutan konteks
- Maksimal 3 instruksi per input
- Jika tidak ada yang cocok, gunakan E (Tahan Posisi) sebagai default
- Kembalikan HANYA JSON array tanpa teks tambahan, tanpa markdown, tanpa backtick

Input pelatih: "{text}"

Format response yang WAJIB diikuti:
[{{"code": "[huruf]", "label": "[nama instruksi]"}}]

Contoh jika ada dua instruksi:
[{{"code": "A", "label": "Maju Serang"}}, {{"code": "E", "label": "Tahan Posisi"}}]"""

def process_speech(text: str) -> dict:
    try:
        prompt   = build_prompt(text)
        response = client.models.generate_content(
            model    = "gemini-3.6-flash",
            contents = prompt
        )

        raw = response.text.strip()
        raw = raw.replace("```json", "").replace("```", "").strip()

        result = json.loads(raw)

        # Pastikan result adalah list
        if not isinstance(result, list):
            result = [result]

        # Validasi setiap item
        validated = []
        for item in result:
            code = item.get("code", "E").upper()
            if code not in TACTICAL_MAPPING:
                code = "E"
            validated.append({
                "code"  : code,
                "label" : TACTICAL_MAPPING[code]["label"],
            })

        # Batasi maksimal 3
        validated = validated[:3]

        return {"results": validated, "status": "ok", "message": ""}

    except json.JSONDecodeError:
        return {
            "results": [{"code": "E", "label": "Tahan Posisi"}],
            "status" : "error",
            "message": "Format response Gemini tidak valid"
        }

    except Exception as e:
        return {
            "results": [{"code": "E", "label": "Tahan Posisi"}],
            "status" : "error",
            "message": str(e)
        }