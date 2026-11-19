"""
Deterministic Test Data Generators for Gesto Pago E2E Tests.
Generates RFC, CURP, account numbers, unique customer profiles,
and MediaPipe biometric vector embeddings with mathematical cosine similarity.
"""
import math
import json
import random
import time
from typing import List, Tuple, Dict, Any, Optional


def calculate_cosine_similarity(vec_a: List[float], vec_b: List[float]) -> float:
    """Computes exact cosine similarity between two float vectors."""
    if len(vec_a) != len(vec_b) or not vec_a:
        return 0.0
    dot_product = sum(a * b for a, b in zip(vec_a, vec_b))
    norm_a = math.sqrt(sum(a * a for a in vec_a))
    norm_b = math.sqrt(sum(b * b for b in vec_b))
    if norm_a == 0.0 or norm_b == 0.0:
        return 0.0
    return dot_product / (norm_a * norm_b)


# Deterministic MediaPipe Vector Embeddings
# Reference 5D vector representing facial landmarks
REFERENCE_EMBEDDING_5D: List[float] = [0.12, -0.45, 0.89, -0.05, 0.67]

# Similar vector (cosine similarity ~ 0.99 > 0.80 threshold)
SIMILAR_EMBEDDING_5D: List[float] = [0.13, -0.44, 0.88, -0.04, 0.68]

# Dissimilar vector (orthogonal / opposite, cosine similarity < 0.20 < 0.80 threshold)
DISSIMILAR_EMBEDDING_5D: List[float] = [-0.85, 0.90, -0.70, 0.40, -0.60]

# Pre-formatted JSON strings for API payloads
REFERENCE_EMBEDDING_STR: str = json.dumps(REFERENCE_EMBEDDING_5D)
SIMILAR_EMBEDDING_STR: str = json.dumps(SIMILAR_EMBEDDING_5D)
DISSIMILAR_EMBEDDING_STR: str = json.dumps(DISSIMILAR_EMBEDDING_5D)


def generate_curp(
    apellido_paterno: str = "PEREZ",
    apellido_materno: str = "LOPEZ",
    nombre: str = "JUAN",
    fecha_nacimiento: str = "1992-05-20",  # YYYY-MM-DD
    sexo: str = "H",
    estado: str = "DF",
    unique_suffix: str = "09",
) -> str:
    """Generates a valid 18-character Mexican CURP."""
    paterno_clean = apellido_paterno.upper().replace(" ", "")
    materno_clean = apellido_materno.upper().replace(" ", "")
    nombre_clean = nombre.upper().replace(" ", "")

    c1 = paterno_clean[0] if paterno_clean else "X"
    # first internal vowel
    vowels = "AEIOU"
    c2 = "X"
    for ch in paterno_clean[1:]:
        if ch in vowels:
            c2 = ch
            break
    c3 = materno_clean[0] if materno_clean else "X"
    c4 = nombre_clean[0] if nombre_clean else "X"

    parts = fecha_nacimiento.split("-")
    yy = parts[0][2:]
    mm = parts[1]
    dd = parts[2]
    date_str = f"{yy}{mm}{dd}"

    # internal consonants
    consonants = "BCDFGHJKLMNPQRSTVWXYZ"
    c14 = next((ch for ch in paterno_clean[1:] if ch in consonants), "X")
    c15 = next((ch for ch in materno_clean[1:] if ch in consonants), "X")
    c16 = next((ch for ch in nombre_clean[1:] if ch in consonants), "X")

    curp_prefix = f"{c1}{c2}{c3}{c4}{date_str}{sexo.upper()}{estado.upper()}{c14}{c15}{c16}"
    # Ensure length 18
    return f"{curp_prefix[:16]}{unique_suffix[:2].upper()}"


def generate_rfc(
    apellido_paterno: str = "PEREZ",
    apellido_materno: str = "LOPEZ",
    nombre: str = "JUAN",
    fecha_nacimiento: str = "1992-05-20",
    homoclave: str = "HDF",
) -> str:
    """Generates a valid 13-character Mexican RFC for persona física."""
    paterno_clean = apellido_paterno.upper().replace(" ", "")
    materno_clean = apellido_materno.upper().replace(" ", "")
    nombre_clean = nombre.upper().replace(" ", "")

    c1 = paterno_clean[0] if paterno_clean else "X"
    vowels = "AEIOU"
    c2 = next((ch for ch in paterno_clean[1:] if ch in vowels), "X")
    c3 = materno_clean[0] if materno_clean else "X"
    c4 = nombre_clean[0] if nombre_clean else "X"

    parts = fecha_nacimiento.split("-")
    yy = parts[0][2:]
    mm = parts[1]
    dd = parts[2]
    date_str = f"{yy}{mm}{dd}"

    rfc_10 = f"{c1}{c2}{c3}{c4}{date_str}"
    return f"{rfc_10}{homoclave[:3].upper()}"


def generate_unique_phone() -> str:
    """Generates a random 10-digit mobile phone starting with 55."""
    random_digits = "".join(random.choice("0123456789") for _ in range(8))
    return f"55{random_digits}"


def generate_account_number(seed_index: int = 1) -> str:
    """Generates a valid 16-digit bank account number."""
    base = 1000000001000000 + seed_index
    return str(base)


def generate_unique_customer_payload(index: Optional[int] = None) -> Dict[str, Any]:
    """Generates complete payload for POST /clientes."""
    idx = index if index is not None else int(time.time() * 1000) % 100000
    suffix = f"{idx % 90 + 10}"
    nombre = f"TestUser{idx}"
    apellido_paterno = "Perez"
    apellido_materno = "Lopez"
    fecha_nac = "1992-05-20"
    email = f"testuser.{idx}@gestopago.test"
    phone = generate_unique_phone()

    curp = generate_curp(apellido_paterno, apellido_materno, nombre, fecha_nac, "H", "DF", suffix)
    rfc = generate_rfc(apellido_paterno, apellido_materno, nombre, fecha_nac, f"X{suffix[:2]}")

    return {
        "nombre": nombre,
        "apellidoPaterno": apellido_paterno,
        "apellidoMaterno": apellido_materno,
        "fechaNacimiento": fecha_nac,
        "curp": curp,
        "rfc": rfc,
        "correoElectronico": email,
        "telefonoMovil": phone,
        "saldoInicial": 1500.00,
        "password": "PasswordSeguro123*",
        "biometricoFacialEmbedding": REFERENCE_EMBEDDING_STR,
        "domicilios": [
            {
                "calle": "Av. Insurgentes Sur",
                "numeroExterior": "1602",
                "numeroInterior": "Piso 4",
                "colonia": "Credito Constructor",
                "codigoPostal": "03940",
                "alcaldiaMunicipio": "Benito Juarez",
                "estado": "Ciudad de Mexico",
                "pais": "Mexico",
            }
        ],
    }
