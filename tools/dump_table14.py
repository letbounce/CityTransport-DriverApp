# -*- coding: utf-8 -*-
from docx import Document
from pathlib import Path

doc = Document(r"d:\Downloads\Коксюк_Диплом.docx")
t = doc.tables[14]
lines = []
for ri, row in enumerate(t.rows):
    lines.append(f"ROW{ri}: " + " || ".join(c.text for c in row.cells))
Path(r"g:\AndroidStudioProjects\docs\_table14.txt").write_text("\n".join(lines), encoding="utf-8")
print("rows", len(t.rows))
