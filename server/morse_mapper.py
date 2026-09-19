# morse_mapper.py

TACTICAL_MAPPING = {
    "A": {
        "label": "Maju Serang",
        "keywords": ["maju", "serang", "attack", "naik"],
    },
    "B": {
        "label": "Bertahan",
        "keywords": ["bertahan", "defend", "mundur", "balik"],
    },
    "C": {
        "label": "Geser Kiri",
        "keywords": ["kiri", "geser kiri", "left"],
    },
    "D": {
        "label": "Geser Kanan",
        "keywords": ["kanan", "geser kanan", "right"],
    },
    "E": {
        "label": "Tahan Posisi",
        "keywords": ["tahan", "diam", "posisi", "hold"],
    },
    "F": {
        "label": "Oper Bola",
        "keywords": ["oper", "passing", "pass", "lempar"],
    },
    "G": {
        "label": "Kembali ke Formasi",
        "keywords": ["formasi", "kembali", "reset", "posisi awal"],
    },
    "H": {
        "label": "Cetak Gol",
        "keywords": ["tembak", "shoot", "gol", "shooting"],
    },
}

def get_mapping_description() -> str:
    lines = []
    for code, data in TACTICAL_MAPPING.items():
        lines.append(f"{code} = {data['label']}")
    return "\n".join(lines)