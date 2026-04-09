import json

BASE_URL = "{{baseUrl}}"


def make_url(path_parts):
    return {
        "raw": BASE_URL + "/api/" + "/".join(path_parts),
        "host": [BASE_URL],
        "path": ["api"] + path_parts
    }


def auth_header():
    return [
        {"key": "Authorization", "value": "Bearer {{token}}"},
        {"key": "Content-Type", "value": "application/json"}
    ]


def status_test(codes=None):
    if codes is None:
        codes = [200, 201]
    return {
        "listen": "test",
        "script": {
            "type": "text/javascript",
            "exec": [
                "pm.test('Status OK', () => pm.expect(pm.response.code).to.be.oneOf([" +
                ",".join(str(c) for c in codes) + "]));"
            ]
        }
    }


def status_and_save_test(var_name, json_path="pm.response.json().data.id", codes=None):
    if codes is None:
        codes = [200, 201]
    return {
        "listen": "test",
        "script": {
            "type": "text/javascript",
            "exec": [
                "pm.test('Status OK', () => pm.expect(pm.response.code).to.be.oneOf([" +
                ",".join(str(c) for c in codes) + "]));",
                "const r = pm.response.json();",
                "pm.collectionVariables.set('" + var_name + "', " + json_path + ");"
            ]
        }
    }


def post_item(name, path_parts, body_dict, events=None):
    return {
        "name": name,
        "event": events or [status_test()],
        "request": {
            "method": "POST",
            "header": auth_header(),
            "body": {"mode": "raw", "raw": json.dumps(body_dict, ensure_ascii=False, indent=2)},
            "url": make_url(path_parts)
        }
    }


def get_item(name, path_parts, events=None, query=None):
    url = make_url(path_parts)
    if query:
        url["raw"] = url["raw"] + "?" + "&".join(k + "=" + str(v) for k, v in query.items())
        url["query"] = [{"key": k, "value": str(v)} for k, v in query.items()]
    return {
        "name": name,
        "event": events or [status_test()],
        "request": {
            "method": "GET",
            "header": auth_header(),
            "url": url
        }
    }


# ─── 00. Auth ───────────────────────────────────────────────────────────────
auth_folder = {
    "name": "00. Auth",
    "item": [
        {
            "name": "Login",
            "event": [{
                "listen": "test",
                "script": {
                    "type": "text/javascript",
                    "exec": [
                        "pm.test('Status OK', () => pm.expect(pm.response.code).to.be.oneOf([200,201]));",
                        "const r = pm.response.json();",
                        "pm.collectionVariables.set('token', r.data.accessToken);"
                    ]
                }
            }],
            "request": {
                "method": "POST",
                "header": [{"key": "Content-Type", "value": "application/json"}],
                "body": {
                    "mode": "raw",
                    "raw": json.dumps({"username": "admin", "password": "admin123"}, indent=2)
                },
                "url": make_url(["auth", "login"])
            }
        }
    ]
}

# ─── 01. Proveedores ─────────────────────────────────────────────────────────
proveedores_raw = [
    ("p01", "Textiles La Consolata S.A.", "Calle 15 #45-22, Bogota", "3001234567", "contacto@textileslc.com"),
    ("p02", "Distribuidora Textil Andina", "Av. Americas #68-30, Bogota", "3109876543", "ventas@textilsandina.com"),
    ("p03", "Insumos y Telas del Norte Ltda.", "Carrera 30 #12-45, Medellin", "3205551234", "info@insumoselnorte.com"),
]

prov_items = []
for var, nombre, direccion, telefono, email in proveedores_raw:
    body = {"nombre": nombre, "direccion": direccion, "telefono": telefono, "email": email}
    item = post_item("Crear Proveedor - " + nombre, ["proveedores"], body,
                     events=[status_and_save_test(var)])
    prov_items.append(item)

proveedores_folder = {"name": "01. Proveedores", "item": prov_items}

# ─── 02. Insumos ─────────────────────────────────────────────────────────────
insumos_data = [
    ("i01", "Tela Lacoste Blanco", "metros", 50),
    ("i02", "Tela Lacoste Azul Turqui", "metros", 50),
    ("i03", "Tela RiB Azul Turqui", "metros", 20),
    ("i04", "Lino Estres Azul Turqui", "metros", 30),
    ("i05", "Tela Dacron Azul Navy", "metros", 40),
    ("i06", "Hilo Poliester Blanco", "conos", 10),
    ("i07", "Hilo Poliester Azul", "conos", 10),
    ("i08", "Botones Blancos 14mm", "unidades", 200),
    ("i09", "Cremallera YKK 20cm Azul", "unidades", 50),
    ("i10", "Elastico 2cm Blanco", "metros", 30),
    ("i11", "Entretela Fusionable", "metros", 20),
    ("i12", "Etiquetas de Talla", "unidades", 100),
]

insumo_items = []
for var, nombre, unidad, stock_min in insumos_data:
    body = {"nombre": nombre, "unidadMedida": unidad, "stockMinimo": stock_min, "stockActual": 0}
    item = post_item("Crear Insumo - " + nombre, ["insumos"], body,
                     events=[status_and_save_test(var)])
    insumo_items.append(item)

insumos_folder = {"name": "02. Insumos", "item": insumo_items}

# ─── 03. Colegios ────────────────────────────────────────────────────────────
colegios_data = [
    ("c01", "La Consolata", "Calle 80 #20-45, Bogota"),
    ("c02", "San Lucas", "Carrera 15 #34-12, Bogota"),
    ("c03", "Jhon F Kennedy", "Av. Kennedy #50-30, Bogota"),
]

colegio_items = []
for var, nombre, direccion in colegios_data:
    body = {"nombre": nombre, "direccion": direccion}
    item = post_item("Crear Colegio - " + nombre, ["colegios"], body,
                     events=[status_and_save_test(var)])
    colegio_items.append(item)

colegios_folder = {"name": "03. Colegios", "item": colegio_items}


# ─── 04. Uniformes ────────────────────────────────────────────────────────────
def ir(insumo_var, cantidad, talla):
    return {"insumoId": "{{" + insumo_var + "}}", "cantidad": cantidad, "talla": talla}


uniformes_data = [
    {
        "var": "u01",
        "nombre": "Sueter Diario Masculino",
        "tipo": "Sueter",
        "genero": "Masculino",
        "colegio": "c01",
        "insumos": [
            ir("i01", 1.2, "S"), ir("i06", 0.3, "S"), ir("i12", 1, "S"),
            ir("i01", 1.4, "M"), ir("i06", 0.3, "M"), ir("i12", 1, "M"),
            ir("i01", 1.6, "L"), ir("i06", 0.4, "L"), ir("i12", 1, "L"),
            ir("i01", 1.8, "XL"), ir("i06", 0.5, "XL"), ir("i12", 1, "XL"),
        ]
    },
    {
        "var": "u02",
        "nombre": "Pantalon Diario Masculino",
        "tipo": "Pantalon",
        "genero": "Masculino",
        "colegio": "c01",
        "insumos": [
            ir("i04", 1.2, "06-08"), ir("i07", 0.3, "06-08"), ir("i09", 1, "06-08"), ir("i12", 1, "06-08"),
            ir("i04", 1.4, "10-12"), ir("i07", 0.3, "10-12"), ir("i09", 1, "10-12"), ir("i12", 1, "10-12"),
            ir("i04", 1.6, "14-16"), ir("i07", 0.4, "14-16"), ir("i09", 1, "14-16"), ir("i12", 1, "14-16"),
        ]
    },
    {
        "var": "u03",
        "nombre": "Camisa Masculina",
        "tipo": "Camisa",
        "genero": "Masculino",
        "colegio": "c02",
        "insumos": [
            ir("i01", 1.1, "S"), ir("i06", 0.2, "S"), ir("i08", 6, "S"), ir("i12", 1, "S"),
            ir("i01", 1.3, "M"), ir("i06", 0.2, "M"), ir("i08", 6, "M"), ir("i12", 1, "M"),
            ir("i01", 1.5, "L"), ir("i06", 0.3, "L"), ir("i08", 6, "L"), ir("i12", 1, "L"),
        ]
    },
    {
        "var": "u04",
        "nombre": "Falda Femenina",
        "tipo": "Falda",
        "genero": "Femenino",
        "colegio": "c02",
        "insumos": [
            ir("i04", 0.9, "06-08"), ir("i07", 0.2, "06-08"), ir("i10", 0.5, "06-08"), ir("i12", 1, "06-08"),
            ir("i04", 1.0, "10-12"), ir("i07", 0.2, "10-12"), ir("i10", 0.5, "10-12"), ir("i12", 1, "10-12"),
        ]
    },
    {
        "var": "u05",
        "nombre": "Camiseta Polo Unisex",
        "tipo": "Camiseta",
        "genero": None,
        "colegio": "c03",
        "insumos": [
            ir("i02", 0.8, "S"), ir("i06", 0.2, "S"), ir("i12", 1, "S"),
            ir("i02", 1.0, "M"), ir("i06", 0.2, "M"), ir("i12", 1, "M"),
            ir("i02", 1.2, "L"), ir("i06", 0.3, "L"), ir("i12", 1, "L"),
            ir("i02", 1.4, "XL"), ir("i06", 0.3, "XL"), ir("i12", 1, "XL"),
        ]
    },
]

uniforme_items = []
for u in uniformes_data:
    body = {
        "nombre": u["nombre"],
        "tipo": u["tipo"],
        "genero": u["genero"],
        "colegioId": "{{" + u["colegio"] + "}}",
        "insumosRequeridos": u["insumos"]
    }
    item = post_item("Crear Uniforme - " + u["nombre"], ["uniformes"], body,
                     events=[status_and_save_test(u["var"])])
    uniforme_items.append(item)

uniformes_folder = {"name": "04. Uniformes", "item": uniforme_items}

# ─── 05. Consultas GET ────────────────────────────────────────────────────────
consultas_items = [
    get_item("GET Uniformes por Colegio c01 La Consolata", ["uniformes", "colegio", "{{c01}}"]),
    get_item("GET Tallas disponibles Sueter u01", ["uniformes", "{{u01}}", "tallas"]),
    get_item("GET Tallas disponibles Pantalon u02", ["uniformes", "{{u02}}", "tallas"]),
    get_item("GET Todos los Colegios", ["colegios"]),
]

consultas_folder = {"name": "05. Consultas GET", "item": consultas_items}


# ─── 06. Entradas de Stock ────────────────────────────────────────────────────
def ed(insumo_var, cantidad):
    return {"insumoId": "{{" + insumo_var + "}}", "cantidad": cantidad}


entrada1_body = {
    "proveedorId": "{{p01}}",
    "observaciones": "Entrada inicial stock La Consolata",
    "detalles": [
        ed("i01", 500),
        ed("i04", 400),
        ed("i06", 50),
        ed("i07", 50),
        ed("i09", 300),
        ed("i12", 500),
    ]
}

entrada2_body = {
    "proveedorId": "{{p02}}",
    "observaciones": "Entrada inicial stock San Lucas y Kennedy",
    "detalles": [
        ed("i02", 300),
        ed("i08", 1000),
        ed("i10", 200),
    ]
}

entradas_items = [
    post_item("Crear Entrada 1 - La Consolata (p01)", ["entradas"], entrada1_body,
              events=[status_and_save_test("entradaId")]),
    post_item("Confirmar Entrada 1", ["entradas", "{{entradaId}}", "confirmar"], {},
              events=[status_test()]),
    post_item("Crear Entrada 2 - San Lucas / Kennedy (p02)", ["entradas"], entrada2_body,
              events=[status_and_save_test("entradaId2")]),
    post_item("Confirmar Entrada 2", ["entradas", "{{entradaId2}}", "confirmar"], {},
              events=[status_test()]),
]

entradas_folder = {"name": "06. Entradas de Stock", "item": entradas_items}

# ─── 07. Calculadora ─────────────────────────────────────────────────────────
calc_verificar_post_body = {
    "uniformeId": "{{u01}}",
    "cantidad": 30,
    "talla": "M"
}

calc_pedido_a_body = {
    "colegioId": "{{c01}}",
    "cantidad": 20,
    "talla": "M"
}

calc_pedido_b_body = {
    "prendas": [
        {"uniformeId": "{{u01}}", "cantidad": 25, "talla": "M"},
        {"uniformeId": "{{u02}}", "cantidad": 25, "talla": "10-12"},
        {"uniformeId": "{{u03}}", "cantidad": 15, "talla": "L"}
    ]
}

calc_verificar_get = get_item(
    "GET Verificar Disponibilidad Pantalon u02 talla 10-12",
    ["calculadora", "verificar", "{{u02}}"],
    query={"cantidad": 20, "talla": "10-12"}
)

calculadora_items = [
    post_item("POST Verificar Disponibilidad Sueter u01 talla M", ["calculadora", "verificar"], calc_verificar_post_body),
    calc_verificar_get,
    post_item("POST Calcular Pedido Modo A colegioId + talla", ["calculadora", "pedido"], calc_pedido_a_body),
    post_item("POST Calcular Pedido Modo B lista prendas con tallas", ["calculadora", "pedido"], calc_pedido_b_body),
]

calculadora_folder = {"name": "07. Calculadora", "item": calculadora_items}

# ─── 08. Pedidos — Flujo Completo ─────────────────────────────────────────────
pedido1_body = {
    "colegioId": "{{c01}}",
    "fechaEstimadaEntrega": "2027-06-30",
    "observaciones": "Pedido de prueba tallas mixtas",
    "detalles": [
        {"uniformeId": "{{u01}}", "cantidad": 20, "talla": "M"},
        {"uniformeId": "{{u02}}", "cantidad": 20, "talla": "10-12"}
    ]
}

pedido_nuevo_colegio_body = {
    "nuevoColegio": {"nombre": "Colegio La Esperanza", "direccion": "Calle 50 #20-15"},
    "fechaEstimadaEntrega": "2027-08-15",
    "detalles": [
        {"uniformeId": "{{u05}}", "cantidad": 10, "talla": "S"}
    ]
}

pedidos_items = [
    post_item("Crear Pedido colegioId + tallas mixtas", ["pedidos"], pedido1_body,
              events=[status_and_save_test("pedidoId")]),
    post_item("Calcular Disponibilidad del Pedido", ["pedidos", "{{pedidoId}}", "calcular"], {},
              events=[status_test()]),
    post_item("Confirmar Pedido", ["pedidos", "{{pedidoId}}", "confirmar"], {},
              events=[status_test()]),
    post_item("Iniciar Produccion", ["pedidos", "{{pedidoId}}", "iniciar-produccion"], {},
              events=[status_test()]),
    post_item("Marcar Listo para Entrega", ["pedidos", "{{pedidoId}}", "marcar-listo"], {},
              events=[status_test()]),
    post_item("Marcar como Entregado", ["pedidos", "{{pedidoId}}", "entregar"], {},
              events=[status_test()]),
    get_item("GET Pedido Completo con salidaId", ["pedidos", "{{pedidoId}}"]),
    post_item("Crear Pedido nuevoColegio inline La Esperanza", ["pedidos"], pedido_nuevo_colegio_body,
              events=[status_and_save_test("pedidoId2")]),
]

pedidos_folder = {"name": "08. Pedidos - Flujo Completo", "item": pedidos_items}

# ─── 09. Reportes ────────────────────────────────────────────────────────────
reportes_specs = [
    ("GENERAL", None),
    ("ENTRADAS", None),
    ("PEDIDOS", None),
    ("ROTACION", None),
    ("STOCK_BAJO", None),
    ("ENTRADAS", "p01"),
]

reporte_items = []
for tipo, proveedor_var in reportes_specs:
    body = {
        "tipoInforme": tipo,
        "fechaInicio": "2025-01-01",
        "fechaFin": "2027-12-31"
    }
    if proveedor_var:
        body["proveedorId"] = "{{" + proveedor_var + "}}"
        name = "Reporte " + tipo + " - Filtro Proveedor p01"
    else:
        name = "Reporte " + tipo
    reporte_items.append(post_item(name, ["reportes"], body))

reportes_folder = {"name": "09. Reportes", "item": reporte_items}

# ─── Collection Variables ─────────────────────────────────────────────────────
variables = [
    {"key": "baseUrl", "value": "http://localhost:8080", "type": "string"},
    {"key": "token", "value": "", "type": "string"},
    {"key": "p01", "value": "", "type": "string"},
    {"key": "p02", "value": "", "type": "string"},
    {"key": "p03", "value": "", "type": "string"},
    {"key": "i01", "value": "", "type": "string"},
    {"key": "i02", "value": "", "type": "string"},
    {"key": "i03", "value": "", "type": "string"},
    {"key": "i04", "value": "", "type": "string"},
    {"key": "i05", "value": "", "type": "string"},
    {"key": "i06", "value": "", "type": "string"},
    {"key": "i07", "value": "", "type": "string"},
    {"key": "i08", "value": "", "type": "string"},
    {"key": "i09", "value": "", "type": "string"},
    {"key": "i10", "value": "", "type": "string"},
    {"key": "i11", "value": "", "type": "string"},
    {"key": "i12", "value": "", "type": "string"},
    {"key": "c01", "value": "", "type": "string"},
    {"key": "c02", "value": "", "type": "string"},
    {"key": "c03", "value": "", "type": "string"},
    {"key": "u01", "value": "", "type": "string"},
    {"key": "u02", "value": "", "type": "string"},
    {"key": "u03", "value": "", "type": "string"},
    {"key": "u04", "value": "", "type": "string"},
    {"key": "u05", "value": "", "type": "string"},
    {"key": "entradaId", "value": "", "type": "string"},
    {"key": "entradaId2", "value": "", "type": "string"},
    {"key": "pedidoId", "value": "", "type": "string"},
    {"key": "pedidoId2", "value": "", "type": "string"},
]

# ─── Full Collection ──────────────────────────────────────────────────────────
collection = {
    "info": {
        "name": "\U0001f9ea Costusoft v2 \u2014 Pruebas con Tallas",
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
        "description": "Coleccion completa para el sistema de inventario Costusoft v2. Nueva arquitectura: Uniforme sin talla propia, talla por InsumoRequerido."
    },
    "variable": variables,
    "item": [
        auth_folder,
        proveedores_folder,
        insumos_folder,
        colegios_folder,
        uniformes_folder,
        consultas_folder,
        entradas_folder,
        calculadora_folder,
        pedidos_folder,
        reportes_folder,
    ]
}

output_path = r'C:\Users\Gerson Ruiz\Documents\MOISES RUIZ\costusoft\costusoft-v2-tallas.postman_collection.json'

with open(output_path, 'w', encoding='utf-8') as f:
    json.dump(collection, f, ensure_ascii=False, indent=2)

print("Written!")

# Quick validation
with open(output_path, 'r', encoding='utf-8') as f:
    loaded = json.load(f)

folders = [item["name"] for item in loaded["item"]]
print("Folders:", folders)
total_requests = sum(len(folder.get("item", [])) for folder in loaded["item"])
print("Total requests:", total_requests)
print("Variables defined:", len(loaded["variable"]))
