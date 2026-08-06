#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Asil Kurt'un kurt adam dokusunu uretir.

    python tools/kurt-adam-doku.py

Cikti:
    clientmod/src/main/resources/assets/hmmo/textures/entity/kurt_adam.png

NEDEN URETILIYOR DA ELLE CIZILMIYOR
-----------------------------------
`tasarim/kurt-adam/texture.png` bitmis bir 128x256 sayfa olarak verildi, ama
hangi bolgenin hangi yuze denk geldigini soyleyen model dosyasi (Blockbench
projesi) yok. Bir kutu UV yerlesimi goruntuden geri cikarilamaz: adalar
birbirine bitisik, ve yanlis tahmin edilen tek bir offset butun modeli bozuk
gosterir - Faz 24'te tam olarak bu oldu.

Bu yuzden Faz 25 yonu tersine cevirdi: yerlesimi WerewolfModel.java tanimliyor,
bu betik de dokuyu O yerlesime gore uretiyor. Ikisi ayni tabloyu paylastigi
surece model asla bozuk gorunemez.

Renkler uydurulmadi: hepsi kaynak sanatin kendi paletinden alindi
(`tasarim/kurt-adam/texture.png` uzerinde en cok gecen tonlar).

DIKKAT: Buradaki PART tablosu ile WerewolfModel.java'daki texOffs cagrilari
AYNI olmak zorundadir. Birini degistiren digerini de degistirir.
"""

import os
import struct
import zlib

# --- Palet: kaynak dokudan sayilarak cikarildi -------------------------------
FUR_DARK       = (0x24, 0x16, 0x0C)   # sirt, kafa ustu, dis yuzeyler
FUR_DARKER     = (0x1A, 0x0F, 0x09)   # golgeler
FUR_MID        = (0x37, 0x21, 0x13)   # yanlar
FUR_LIGHT      = (0x5E, 0x41, 0x2F)   # karin, ic bacak, agiz alti
FUR_LIGHTER    = (0x6B, 0x4E, 0x3B)   # gogus tuyu, kas parlaklari
CREAM          = (0xF7, 0xF0, 0xD4)   # disler, pence uclari
CREAM_SHADE    = (0xD8, 0xCE, 0xAE)
NOSE_BLACK     = (0x0A, 0x07, 0x05)
MOUTH_RED      = (0x97, 0x28, 0x28)
MOUTH_RED_DEEP = (0x5E, 0x18, 0x18)
EYE_RED        = (0xC0, 0x30, 0x2C)
EYE_GLOW       = (0xE8, 0x5A, 0x50)

TEXTURE_WIDTH = 128
TEXTURE_HEIGHT = 128

# --- Parca tablosu -----------------------------------------------------------
# ad: (u, v, genislik, yukseklik, derinlik)
# Kutu UV adasi 2*(w+d) genislikte, (d+h) yuksekliktedir:
#         +-----+-----+
#         | ust | alt |          <- yukseklik d
#   +-----+-----+-----+-----+
#   | sag | on  | sol | arka|    <- yukseklik h
#   +-----+-----+-----+-----+
#   genislikler: d, w, d, w
PARTS = {
    "govde":   (0, 0, 12, 13, 9),
    "kafa":    (44, 0, 8, 9, 8),
    "kuyruk":  (78, 0, 4, 4, 11),
    "burun":   (0, 24, 5, 4, 7),
    "altcene": (26, 24, 5, 2, 6),
    "kulak":   (50, 24, 3, 4, 2),
    "ustkol":  (62, 24, 4, 8, 5),
    "onkol":   (82, 24, 4, 8, 4),
    "pence":   (100, 24, 5, 3, 6),
    "uyluk":   (0, 40, 5, 5, 6),
    "baldir":  (24, 40, 4, 5, 4),
    "ayak":    (42, 40, 4, 3, 7),
    "yele":    (66, 40, 14, 6, 11),
}

FACES = ("ust", "alt", "sag", "on", "sol", "arka")


def face_rect(part, face):
    """Bir yuzun sayfadaki dikdortgeni: (x, y, genislik, yukseklik)."""
    u, v, w, h, d = PARTS[part]
    if face == "ust":
        return (u + d, v, w, d)
    if face == "alt":
        return (u + d + w, v, w, d)
    if face == "sag":
        return (u, v + d, d, h)
    if face == "on":
        return (u + d, v + d, w, h)
    if face == "sol":
        return (u + d + w, v + d, d, h)
    if face == "arka":
        return (u + 2 * d + w, v + d, w, h)
    raise ValueError(face)


# --- Cizim yardimcilari ------------------------------------------------------
class Sheet:
    def __init__(self, width, height):
        self.width = width
        self.height = height
        self.data = bytearray(width * height * 4)

    def put(self, x, y, colour, alpha=255):
        if not (0 <= x < self.width and 0 <= y < self.height):
            return
        offset = (y * self.width + x) * 4
        self.data[offset] = colour[0]
        self.data[offset + 1] = colour[1]
        self.data[offset + 2] = colour[2]
        self.data[offset + 3] = alpha

    def save(self, path):
        raw = bytearray()
        for y in range(self.height):
            raw.append(0)
            raw += self.data[y * self.width * 4:(y + 1) * self.width * 4]

        def chunk(tag, payload):
            return (struct.pack(">I", len(payload)) + tag + payload
                    + struct.pack(">I", zlib.crc32(tag + payload) & 0xFFFFFFFF))

        png = b"\x89PNG\r\n\x1a\n"
        png += chunk(b"IHDR", struct.pack(">IIBBBBB", self.width, self.height, 8, 6, 0, 0, 0))
        png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
        png += chunk(b"IEND", b"")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as handle:
            handle.write(png)


def noise(x, y, salt=0):
    """Sabit, tekrar uretilebilir gurultu: ayni piksel her calistirmada ayni."""
    value = (x * 374761393 + y * 668265263 + salt * 2147483647) & 0xFFFFFFFF
    value = (value ^ (value >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((value ^ (value >> 16)) & 0xFFFF) / 65535.0


def blend(a, b, t):
    return tuple(int(round(a[index] + (b[index] - a[index]) * t)) for index in range(3))


def fur(sheet, rect, base, shade, salt=0, streaks=True):
    """Tuy: taban rengin etrafinda dagilan noktalar + dikey tuy cizgileri."""
    x0, y0, width, height = rect
    for y in range(height):
        for x in range(width):
            t = noise(x0 + x, y0 + y, salt)
            colour = blend(base, shade, t * 0.85)
            if streaks and noise(x0 + x, y0 + (y // 3), salt + 7) > 0.86:
                colour = blend(colour, FUR_DARKER, 0.55)
            sheet.put(x0 + x, y0 + y, colour)


def gradient(sheet, rect, top, bottom, salt=0):
    """Ustten alta gecen tuy - karin ve ic bacaklar boyle acilir."""
    x0, y0, width, height = rect
    for y in range(height):
        ratio = y / max(1, height - 1)
        row = blend(top, bottom, ratio)
        for x in range(width):
            t = noise(x0 + x, y0 + y, salt)
            sheet.put(x0 + x, y0 + y, blend(row, FUR_DARKER, t * 0.35))


def solid(sheet, rect, colour):
    x0, y0, width, height = rect
    for y in range(height):
        for x in range(width):
            sheet.put(x0 + x, y0 + y, colour)


def dot(sheet, rect, x, y, colour):
    x0, y0, width, height = rect
    if 0 <= x < width and 0 <= y < height:
        sheet.put(x0 + x, y0 + y, colour)


def fill(sheet, rect, x, y, width, height, colour):
    for dy in range(height):
        for dx in range(width):
            dot(sheet, rect, x + dx, y + dy, colour)


# --- Parcalarin boyanmasi ----------------------------------------------------
def paint_body(sheet):
    """Gogus kafesi: sirt koyu, karin acik, yanlar arada."""
    fur(sheet, face_rect("govde", "ust"), FUR_DARK, FUR_DARKER, 1)
    gradient(sheet, face_rect("govde", "alt"), FUR_LIGHT, FUR_LIGHTER, 2)
    fur(sheet, face_rect("govde", "sag"), FUR_MID, FUR_DARK, 3)
    fur(sheet, face_rect("govde", "sol"), FUR_MID, FUR_DARK, 3)
    fur(sheet, face_rect("govde", "arka"), FUR_DARK, FUR_DARKER, 4)
    # On yuz gogus: ortasi acik, kenarlari koyu - referans cizimdeki gogus tuyu.
    front = face_rect("govde", "on")
    fur(sheet, front, FUR_MID, FUR_DARK, 5)
    _, _, width, height = front
    for y in range(height):
        spread = 3 + (y * 2) // max(1, height)
        for x in range(width // 2 - spread, width // 2 + spread):
            t = noise(x, y, 9)
            dot(sheet, front, x, y, blend(FUR_LIGHT, FUR_LIGHTER, t))


def paint_mane(sheet):
    """Omuz yelesi: her yuzu koyu ve kaba, alti govdeye bakiyor."""
    for face in FACES:
        fur(sheet, face_rect("yele", face), FUR_DARKER, FUR_DARK, 11, streaks=True)
    # Ustune bir kac acik tel: dumduz koyu bir blok yele degil kutu gibi durur.
    top = face_rect("yele", "ust")
    _, _, width, height = top
    for x in range(width):
        if noise(x, 0, 12) > 0.6:
            fill(sheet, top, x, 0, 1, height, blend(FUR_MID, FUR_LIGHT, noise(x, 1, 13)))


def paint_head(sheet):
    fur(sheet, face_rect("kafa", "ust"), FUR_DARK, FUR_DARKER, 21)
    fur(sheet, face_rect("kafa", "alt"), FUR_LIGHT, FUR_MID, 22)
    fur(sheet, face_rect("kafa", "sag"), FUR_MID, FUR_DARK, 23)
    fur(sheet, face_rect("kafa", "sol"), FUR_MID, FUR_DARK, 23)
    fur(sheet, face_rect("kafa", "arka"), FUR_DARK, FUR_DARKER, 24)

    front = face_rect("kafa", "on")
    fur(sheet, front, FUR_MID, FUR_DARK, 25)
    # Kasarasi koyu bir V: kurdun bakisini veren tek sey bu.
    for x in range(8):
        depth = 1 if 2 <= x <= 5 else 0
        fill(sheet, front, x, 1 + depth, 1, 1, FUR_DARKER)
    # Gozler: dis koyu cerceve, ic kirmizi, ustunde parlak bir piksel.
    for eye_x in (1, 5):
        fill(sheet, front, eye_x, 2, 2, 2, NOSE_BLACK)
        fill(sheet, front, eye_x, 2, 2, 1, EYE_RED)
        dot(sheet, front, eye_x + (1 if eye_x == 1 else 0), 2, EYE_GLOW)
    # Alt yarim burnun altinda kalir; agiz acikken gorunen kisim acik tuy.
    for y in range(5, 9):
        for x in range(8):
            dot(sheet, front, x, y, blend(FUR_LIGHT, FUR_MID, noise(x, y, 26)))


def paint_snout(sheet):
    fur(sheet, face_rect("burun", "ust"), FUR_DARK, FUR_DARKER, 31)
    gradient(sheet, face_rect("burun", "alt"), FUR_LIGHT, FUR_LIGHTER, 32)
    fur(sheet, face_rect("burun", "sag"), FUR_MID, FUR_DARK, 33)
    fur(sheet, face_rect("burun", "sol"), FUR_MID, FUR_DARK, 33)
    fur(sheet, face_rect("burun", "arka"), FUR_MID, FUR_DARK, 34)
    # On yuz: siyah burun ve iki burun deligi.
    nose = face_rect("burun", "on")
    solid(sheet, nose, NOSE_BLACK)
    dot(sheet, nose, 1, 1, (0x1F, 0x18, 0x16))
    dot(sheet, nose, 3, 1, (0x1F, 0x18, 0x16))
    # Ust dudak boyunca disler: agiz kapaliyken de gorunen kopek disleri.
    top_lip = face_rect("burun", "sag")
    _, _, _, lip_height = top_lip
    fill(sheet, top_lip, 0, lip_height - 1, 1, 1, CREAM)
    other_lip = face_rect("burun", "sol")
    fill(sheet, other_lip, 0, lip_height - 1, 1, 1, CREAM)


def paint_jaw(sheet):
    """Alt cene: ustu agzin ici (kirmizi), onu dis sirasi."""
    solid(sheet, face_rect("altcene", "ust"), MOUTH_RED)
    inner = face_rect("altcene", "ust")
    _, _, width, height = inner
    for y in range(height):
        for x in range(width):
            if noise(x, y, 41) > 0.65:
                dot(sheet, inner, x, y, MOUTH_RED_DEEP)
    gradient(sheet, face_rect("altcene", "alt"), FUR_LIGHT, FUR_MID, 42)
    fur(sheet, face_rect("altcene", "sag"), FUR_MID, FUR_DARK, 43)
    fur(sheet, face_rect("altcene", "sol"), FUR_MID, FUR_DARK, 43)
    fur(sheet, face_rect("altcene", "arka"), MOUTH_RED_DEEP, MOUTH_RED, 44, streaks=False)
    front = face_rect("altcene", "on")
    fur(sheet, front, FUR_MID, FUR_DARK, 45)
    for x in range(5):
        dot(sheet, front, x, 0, CREAM if x % 2 == 0 else CREAM_SHADE)


def paint_ear(sheet):
    for face in FACES:
        fur(sheet, face_rect("kulak", face), FUR_DARK, FUR_DARKER, 51)
    inner = face_rect("kulak", "on")
    fill(sheet, inner, 1, 1, 1, 3, blend(FUR_LIGHT, MOUTH_RED_DEEP, 0.35))


def paint_arm(sheet):
    fur(sheet, face_rect("ustkol", "ust"), FUR_DARK, FUR_DARKER, 61)
    fur(sheet, face_rect("ustkol", "alt"), FUR_MID, FUR_DARK, 62)
    fur(sheet, face_rect("ustkol", "sag"), FUR_DARK, FUR_DARKER, 63)
    fur(sheet, face_rect("ustkol", "sol"), FUR_MID, FUR_LIGHT, 64)
    fur(sheet, face_rect("ustkol", "on"), FUR_MID, FUR_DARK, 65)
    fur(sheet, face_rect("ustkol", "arka"), FUR_DARK, FUR_DARKER, 66)

    for face in FACES:
        fur(sheet, face_rect("onkol", face), FUR_DARK, FUR_DARKER, 67)
    # On kolun ic yuzu acilir: referansta kolun ic tarafi govde kadar acik.
    gradient(sheet, face_rect("onkol", "sol"), FUR_MID, FUR_LIGHT, 68)


def paint_paw(sheet):
    for face in FACES:
        fur(sheet, face_rect("pence", face), FUR_DARK, FUR_DARKER, 71)
    # Uc tirnak: on yuzun alt kenarindan ve alt yuzun on kenarindan cikar.
    front = face_rect("pence", "on")
    _, _, front_width, front_height = front
    for x in range(front_width):
        if x % 2 == 0:
            fill(sheet, front, x, front_height - 1, 1, 1, CREAM)
    bottom = face_rect("pence", "alt")
    _, _, bottom_width, bottom_height = bottom
    for x in range(bottom_width):
        if x % 2 == 0:
            fill(sheet, bottom, x, 0, 1, 2, CREAM if x % 4 == 0 else CREAM_SHADE)
    for y in range(2, bottom_height):
        for x in range(bottom_width):
            dot(sheet, bottom, x, y, blend(NOSE_BLACK, FUR_DARKER, noise(x, y, 72)))


def paint_leg(sheet):
    fur(sheet, face_rect("uyluk", "ust"), FUR_DARK, FUR_DARKER, 81)
    fur(sheet, face_rect("uyluk", "alt"), FUR_MID, FUR_DARK, 82)
    fur(sheet, face_rect("uyluk", "sag"), FUR_DARK, FUR_DARKER, 83)
    gradient(sheet, face_rect("uyluk", "sol"), FUR_MID, FUR_LIGHT, 84)
    gradient(sheet, face_rect("uyluk", "on"), FUR_MID, FUR_LIGHT, 85)
    fur(sheet, face_rect("uyluk", "arka"), FUR_DARK, FUR_DARKER, 86)

    for face in FACES:
        fur(sheet, face_rect("baldir", face), FUR_DARK, FUR_DARKER, 87)


def paint_foot(sheet):
    for face in FACES:
        fur(sheet, face_rect("ayak", face), FUR_DARK, FUR_DARKER, 91)
    front = face_rect("ayak", "on")
    _, _, width, height = front
    for x in range(width):
        if x % 2 == 0:
            fill(sheet, front, x, height - 1, 1, 1, CREAM)
    bottom = face_rect("ayak", "alt")
    _, _, bottom_width, bottom_height = bottom
    for x in range(bottom_width):
        if x % 2 == 0:
            fill(sheet, bottom, x, 0, 1, 2, CREAM_SHADE)
    for y in range(2, bottom_height):
        for x in range(bottom_width):
            dot(sheet, bottom, x, y, blend(NOSE_BLACK, FUR_DARKER, noise(x, y, 92)))


def paint_tail(sheet):
    for face in FACES:
        fur(sheet, face_rect("kuyruk", face), FUR_DARK, FUR_DARKER, 101)
    # Ucu acik: kurt kuyrugunun ucu her zaman govdesinden farkli.
    for face in ("ust", "sag", "sol", "arka"):
        rect = face_rect("kuyruk", face)
        _, _, width, height = rect
        if face in ("ust",):
            fill(sheet, rect, 0, height - 3, width, 3, FUR_LIGHT)
        else:
            fill(sheet, rect, width - 3, 0, 3, height, FUR_LIGHT)


def main():
    sheet = Sheet(TEXTURE_WIDTH, TEXTURE_HEIGHT)
    paint_body(sheet)
    paint_mane(sheet)
    paint_head(sheet)
    paint_snout(sheet)
    paint_jaw(sheet)
    paint_ear(sheet)
    paint_arm(sheet)
    paint_paw(sheet)
    paint_leg(sheet)
    paint_foot(sheet)
    paint_tail(sheet)

    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    target = os.path.join(root, "clientmod", "src", "main", "resources",
                          "assets", "hmmo", "textures", "entity", "kurt_adam.png")
    sheet.save(target)
    print("yazildi: " + target)


if __name__ == "__main__":
    main()
