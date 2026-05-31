# -*- coding: utf-8 -*-
from docx import Document
from pathlib import Path

doc = Document(r"d:\Downloads\Коксюк_Диплом.docx")
paras = [p.text.strip() for p in doc.paragraphs if p.text.strip()]
out = Path(r"g:\AndroidStudioProjects\docs\_thesis_snippets.txt")
chunks = [
    ("REFERAT+ANOT (50-90)", 50, 90),
    ("INTRO (145-165)", 145, 165),
    ("CONCLUSIONS (420-435)", 420, 435),
    ("BIB+APP (435-460)", 435, 460),
    ("MATRIX 2.3.3 area", 400, 430),
]
lines = []
for title, a, b in chunks:
    lines.append(f"\n===== {title} =====")
    for i in range(a, min(b, len(paras))):
        lines.append(f"{i:4d}| {paras[i]}")
out.write_text("\n".join(lines), encoding="utf-8")
print("ok", out)
