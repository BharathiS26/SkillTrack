from flask import Flask, render_template, request, jsonify, redirect, url_for, session
from werkzeug.security import generate_password_hash, check_password_hash
import subprocess
import json
import os

app = Flask(__name__)

# Set a real secret in your environment. The fallback is only for local testing.
app.secret_key = os.environ.get("SKILLTRACK_SECRET", "dev-only-change-me")

# Users: register number -> hashed password.
# CHANGE these passwords before you show this to anyone.
USERS = {
    "310625205043": generate_password_hash("change-me-1"),
    "310625205062": generate_password_hash("change-me-2"),
    "admin": generate_password_hash("change-me-admin"),
}


# Compile Java code automatically on server start
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
JAVA_SRC = os.path.join(BASE_DIR, "SkillTrackEngine.java")
JAVA_CLASS = os.path.join(BASE_DIR, "SkillTrackEngine.class")


def compile_java():
    # Compile if the class file is missing OR the .java file was edited after it
    needs_compile = (not os.path.exists(JAVA_CLASS)) or (
        os.path.getmtime(JAVA_SRC) > os.path.getmtime(JAVA_CLASS)
    )
    if needs_compile:
        result = subprocess.run(
            ["javac", "SkillTrackEngine.java", "-d", "."],
            cwd=BASE_DIR, capture_output=True, text=True
        )
        if result.returncode != 0:
            print("JAVA COMPILE FAILED:\n", result.stderr)


compile_java()


@app.route("/")
def index():
    # Already logged in? Skip the login page.
    if "reg_no" in session:
        return redirect(url_for("dashboard"))
    return render_template("index.html")


@app.route("/login", methods=["POST"])
def login():
    data = request.get_json(silent=True) or {}
    reg_no = str(data.get("regNo", "")).strip()
    password = str(data.get("password", ""))

    if not reg_no or not password:
        return jsonify({"status": "error", "message": "Enter both your register number and password."}), 400

    stored_hash = USERS.get(reg_no)
    if stored_hash and check_password_hash(stored_hash, password):
        session.clear()
        session["reg_no"] = reg_no
        return jsonify({"status": "success", "message": "Login successful!", "redirect": url_for("dashboard")})

    # Same message for wrong user and wrong password, so nobody can guess valid numbers
    return jsonify({"status": "error", "message": "Invalid register number or password."}), 401


@app.route("/dashboard")
def dashboard():
    if "reg_no" not in session:
        return redirect(url_for("index"))
    return render_template("dashboard.html", reg_no=session["reg_no"])


@app.route("/calculate", methods=["POST"])
def calculate():
    if "reg_no" not in session:
        return jsonify({"status": "error", "message": "Unauthorized"}), 401

    data = request.get_json(silent=True) or {}

    # Only pass whole numbers to Java, so bad input can't crash it
    def num(key):
        try:
            return str(max(0, int(data.get(key, 0))))
        except (TypeError, ValueError):
            return "0"

    args = [
        "java", "SkillTrackEngine",
        num("techCerts"),
        num("softCerts"),
        num("easyProjects"),
        num("mediumProjects"),
        num("hardProjects"),
        num("leetcode"),
        num("gfg"),
        num("hackerrank"),
    ]

    try:
        result = subprocess.run(
            args, capture_output=True, text=True, cwd=BASE_DIR, timeout=10
        )
    except FileNotFoundError:
        return jsonify({"status": "error", "message": "Java is not installed or not in PATH."}), 500
    except subprocess.TimeoutExpired:
        return jsonify({"status": "error", "message": "Java engine took too long."}), 500

    try:
        java_output = json.loads(result.stdout)
    except Exception:
        return jsonify({
            "status": "error",
            "message": "Java computation failed.",
            "details": result.stderr.strip()[:300]
        }), 500

    if "error" in java_output:
        return jsonify({"status": "error", "message": java_output["error"]}), 400

    return jsonify({"status": "success", "data": java_output})


@app.route("/logout")
def logout():
    session.clear()
    return redirect(url_for("index"))


if __name__ == "__main__":
    app.run(debug=True, port=5000)
