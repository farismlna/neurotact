# morse_mapper.py
# Mapping instruksi taktis ke kode huruf
# PLACEHOLDER - akan diupdate setelah ada instruksi yang benar dari pelatih

TACTICAL_MAPPING = {
    "A": {
        "label": "Maju Serang",
        "keywords": ["maju", "serang", "attack", "naik"],
        "morse": ".-"
    },
    "B": {
        "label": "Bertahan",
        "keywords": ["bertahan", "defend", "mundur", "balik"],
        "morse": "-..."
    },
    "C": {
        "label": "Geser Kiri",
        "keywords": ["kiri", "geser kiri", "left"],
        "morse": "-.-."
    },
    "D": {
        "label": "Geser Kanan",
        "keywords": ["kanan", "geser kanan", "right"],
        "morse": "-.."
    },
    "E": {
        "label": "Tahan Posisi",
        "keywords": ["tahan", "diam", "posisi", "hold"],
        "morse": "."
    },
    "F": {
        "label": "Oper Bola",
        "keywords": ["oper", "passing", "pass", "lempar"],
        "morse": "..-."
    },
    "G": {
        "label": "Kembali ke Formasi",
        "keywords": ["formasi", "kembali", "reset", "posisi awal"],
        "morse": "--."
    },
    "H": {
        "label": "Cetak Gol",
        "keywords": ["tembak", "shoot", "gol", "shooting"],
        "morse": "...."
    },
}

def get_mapping_description():
    """Return deskripsi mapping untuk dimasukkan ke prompt Gemini"""
    lines = []
    for code, data in TACTICAL_MAPPING.items():
        lines.append(f"{code} = {data['label']}")
    return "\n".join(lines)

def get_morse(code: str) -> str:
    """Return kode morse dari huruf"""
    if code in TACTICAL_MAPPING:
        return TACTICAL_MAPPING[code]["morse"]
    return ""