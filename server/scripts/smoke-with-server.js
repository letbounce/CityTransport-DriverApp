/* eslint-disable no-console */
/**
 * Запускає сервер (якщо ще не працює), опційно seed, виконує smoke.js (табл. 3.7).
 */
const path = require("path");
const { spawn } = require("child_process");
const { waitForServer, HEALTH_URL, sleep } = require("./smoke-lib");
const { runSmokeTests } = require("./smoke");

const serverDir = path.join(__dirname, "..");
const runSeed = process.argv.includes("--seed");

async function isServerUp() {
  try {
    const res = await fetch(HEALTH_URL);
    if (!res.ok) return false;
    const body = await res.json();
    return body.status === "ok";
  } catch {
    return false;
  }
}

function runNodeScript(script, args = []) {
  return new Promise((resolve, reject) => {
    const child = spawn(process.execPath, [script, ...args], {
      cwd: serverDir,
      stdio: "inherit",
      env: process.env,
      shell: process.platform === "win32"
    });
    child.on("error", reject);
    child.on("exit", (code) => {
      if (code === 0) resolve();
      else reject(new Error(`${script} exited with code ${code}`));
    });
  });
}

async function main() {
  if (runSeed) {
    console.log("=== npm run seed ===");
    await runNodeScript(path.join("scripts", "seed.js"));
  }

  let serverChild = null;
  const alreadyUp = await isServerUp();

  if (!alreadyUp) {
    console.log("=== Запуск сервера (node index.js) ===");
    serverChild = spawn(process.execPath, ["index.js"], {
      cwd: serverDir,
      stdio: "inherit",
      env: process.env,
      detached: false
    });
    serverChild.on("error", (e) => {
      console.error(e);
      process.exit(1);
    });
    await waitForServer(60000);
    await sleep(500);
  } else {
    console.log("Сервер вже працює:", HEALTH_URL);
  }

  try {
    console.log("\n=== Smoke-тести (табл. 3.7) ===\n");
    await runSmokeTests();
  } finally {
    if (serverChild) {
      console.log("\nЗупинка сервера...");
      serverChild.kill("SIGTERM");
      await sleep(800);
    }
  }
}

main().catch((e) => {
  console.error(e.message || e);
  process.exit(1);
});
