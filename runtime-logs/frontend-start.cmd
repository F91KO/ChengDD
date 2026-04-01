@echo off
cd /d F:\clauseWork\ChengDD\cdd-frontend
"C:\Program Files\nodejs\corepack.cmd" pnpm dev --host 0.0.0.0 --port 4173 >> "F:\clauseWork\ChengDD\runtime-logs\frontend.out.log" 2>> "F:\clauseWork\ChengDD\runtime-logs\frontend.err.log"
