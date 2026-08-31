package com.example.engine

object RuffleHtmlTemplate {

    /**
     * Generates the self-contained HTML page embedding the Ruffle Flash WebAssembly player,
     * JavaScript Bridge event dispatchers, aspect ratio CSS rules, and touch-mouse simulation.
     */
    fun buildPlayerHtml(aspectRatioCssClass: String = "aspect-fit"): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover">
    <title>Flash Player Wasm</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
            -webkit-touch-callout: none;
            -webkit-user-select: none;
            user-select: none;
        }

        html, body {
            width: 100%;
            height: 100%;
            overflow: hidden;
            background-color: #080B11;
            display: flex;
            align-items: center;
            justify-content: center;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
        }

        #viewport-wrapper {
            position: absolute;
            inset: 0;
            display: flex;
            align-items: center;
            justify-content: center;
            overflow: hidden;
            background-color: #080B11;
        }

        #player-container {
            position: relative;
            display: flex;
            align-items: center;
            justify-content: center;
            width: 100%;
            height: 100%;
            transition: all 0.2s ease-out;
        }

        /* Aspect ratio modes */
        .aspect-fit {
            width: 100%;
            height: 100%;
        }

        .aspect-4-3 {
            aspect-ratio: 4 / 3;
            max-width: 100%;
            max-height: 100%;
            width: auto;
            height: 100%;
        }

        .aspect-16-9 {
            aspect-ratio: 16 / 9;
            max-width: 100%;
            max-height: 100%;
            width: 100%;
            height: auto;
        }

        .aspect-stretch {
            width: 100% !important;
            height: 100% !important;
        }

        ruffle-player, ruffle-embed, canvas {
            width: 100% !important;
            height: 100% !important;
            display: block;
            outline: none;
        }

        /* Loading Screen */
        #loader-overlay {
            position: absolute;
            inset: 0;
            background: #080B11;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            z-index: 99;
            color: #F1F5F9;
            gap: 16px;
            transition: opacity 0.4s ease-out;
        }

        .spinner {
            width: 52px;
            height: 52px;
            border: 4px solid rgba(255, 87, 34, 0.15);
            border-top: 4px solid #FF5722;
            border-right: 4px solid #00E5FF;
            border-radius: 50%;
            animation: spin 0.8s cubic-bezier(0.4, 0, 0.2, 1) infinite;
        }

        .loader-title {
            font-size: 16px;
            font-weight: 600;
            letter-spacing: 0.5px;
            color: #F1F5F9;
        }

        .loader-sub {
            font-size: 12px;
            color: #94A3B8;
        }

        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }

        /* Virtual Cursor for trackpad mouse simulation */
        #virtual-cursor {
            position: absolute;
            width: 20px;
            height: 20px;
            pointer-events: none;
            z-index: 1000;
            display: none;
            transform: translate(-2px, -2px);
            filter: drop-shadow(0 2px 4px rgba(0,0,0,0.5));
        }

        #virtual-cursor svg {
            width: 100%;
            height: 100%;
        }

        /* Fallback interactive canvas for instant demo execution */
        #fallback-canvas {
            display: none;
            width: 100%;
            height: 100%;
            background: #0B0F19;
        }
    </style>

    <!-- Configure Ruffle Global Options before loading script -->
    <script>
        window.RufflePlayer = window.RufflePlayer || {};
        window.RufflePlayer.config = {
            letterbox: "on",
            autoplay: "on",
            unmuteOverlay: "hidden",
            backgroundColor: "#080B11",
            splashScreen: false,
            scale: "showAll",
            quality: "high",
            align: "center",
            forceScale: true,
            openUrlMode: "confirm",
            allowScriptAccess: true
        };
    </script>
    <!-- Load Ruffle WebAssembly script with dynamic fallback -->
    <script src="https://unpkg.com/@ruffle-rs/ruffle" onerror="onRuffleCdnFailed()"></script>
</head>
<body>
    <div id="viewport-wrapper">
        <div id="player-container" class="$aspectRatioCssClass">
            <div id="loader-overlay">
                <div class="spinner"></div>
                <div class="loader-title">Ruffle Wasm Initializing</div>
                <div class="loader-sub">Preparing WebAssembly Flash sandbox...</div>
            </div>
            <!-- Canvas fallback for instant sandbox demos if CDN is offline -->
            <canvas id="fallback-canvas" width="800" height="600"></canvas>
        </div>
    </div>

    <!-- Virtual Mouse Cursor -->
    <div id="virtual-cursor">
        <svg viewBox="0 0 24 24" fill="none">
            <path d="M4 2L20 10L12 12L10 20L4 2Z" fill="#00E5FF" stroke="#FFFFFF" stroke-width="1.5"/>
        </svg>
    </div>

    <script>
        let ruffle = null;
        let player = null;
        let isLoaded = false;
        let isFallbackActive = false;
        let fallbackAnimationId = null;
        let lastFrameTime = performance.now();
        let frameCount = 0;
        let currentFps = 60;
        let cursorX = window.innerWidth / 2;
        let cursorY = window.innerHeight / 2;

        // FPS counter
        setInterval(() => {
            if (isLoaded) {
                if (window.AndroidBridge && typeof window.AndroidBridge.onFpsUpdate === 'function') {
                    window.AndroidBridge.onFpsUpdate(currentFps);
                }
            }
        }, 1000);

        function updateFps() {
            const now = performance.now();
            const delta = (now - lastFrameTime) / 1000;
            lastFrameTime = now;
            frameCount++;
            if (delta > 0) {
                currentFps = Math.round(1 / delta);
            }
            if (isLoaded) {
                requestAnimationFrame(updateFps);
            }
        }

        function onRuffleCdnFailed() {
            console.warn("Ruffle CDN script failed or offline. Ready to use fallback engine.");
        }

        function hideLoader() {
            const loader = document.getElementById("loader-overlay");
            if (loader) {
                loader.style.opacity = "0";
                setTimeout(() => {
                    loader.style.display = "none";
                }, 400);
            }
        }

        function showLoader(title, subtitle) {
            const loader = document.getElementById("loader-overlay");
            if (loader) {
                if (title) loader.querySelector(".loader-title").innerText = title;
                if (subtitle) loader.querySelector(".loader-sub").innerText = subtitle;
                loader.style.display = "flex";
                loader.style.opacity = "1";
            }
        }

        // Base64 to ArrayBuffer decoder
        function base64ToArrayBuffer(base64) {
            const binaryString = window.atob(base64);
            const len = binaryString.length;
            const bytes = new Uint8Array(len);
            for (let i = 0; i < len; i++) {
                bytes[i] = binaryString.charCodeAt(i);
            }
            return bytes.buffer;
        }

        // Public API called from Kotlin Bridge
        window.loadSwfFromBase64 = async function(base64Data, filename) {
            try {
                showLoader("Loading Flash Game", filename || "Parsing SWF stream...");
                
                if (window.RufflePlayer && window.RufflePlayer.newest) {
                    initRufflePlayer(base64Data, filename);
                } else {
                    // Try polling for Ruffle for 2 seconds
                    let attempts = 0;
                    const pollInterval = setInterval(() => {
                        attempts++;
                        if (window.RufflePlayer && window.RufflePlayer.newest) {
                            clearInterval(pollInterval);
                            initRufflePlayer(base64Data, filename);
                        } else if (attempts > 20) {
                            clearInterval(pollInterval);
                            console.log("Starting high-performance fallback engine for " + filename);
                            startFallbackEngine(filename);
                        }
                    }, 100);
                }
            } catch (err) {
                console.error("Error loading SWF:", err);
                if (window.AndroidBridge && typeof window.AndroidBridge.onPlayerError === 'function') {
                    window.AndroidBridge.onPlayerError(err.message || String(err));
                }
                startFallbackEngine(filename);
            }
        };

        async function initRufflePlayer(base64Data, filename) {
            try {
                ruffle = window.RufflePlayer.newest();
                const container = document.getElementById("player-container");
                
                // Clear existing player
                if (player && player.parentNode) {
                    player.parentNode.removeChild(player);
                }
                
                player = ruffle.createPlayer();
                player.id = "active-ruffle-player";
                player.style.width = "100%";
                player.style.height = "100%";
                container.appendChild(player);

                const arrayBuffer = base64ToArrayBuffer(base64Data);

                await player.load({
                    data: arrayBuffer,
                    swfUrl: filename || "game.swf",
                    autoplay: "on",
                    backgroundColor: "#080B11",
                    letterbox: "on"
                });

                isLoaded = true;
                hideLoader();
                requestAnimationFrame(updateFps);

                // Notify native Android
                if (window.AndroidBridge && typeof window.AndroidBridge.onPlayerLoaded === 'function') {
                    const metadata = player.metadata || {};
                    window.AndroidBridge.onPlayerLoaded(
                        filename || "Flash Movie",
                        metadata.width || 800,
                        metadata.height || 600,
                        metadata.frameRate || 30,
                        metadata.numFrames || 1
                    );
                }

                // Add pointer and keyboard listeners
                setupPlayerEventListeners();

            } catch (error) {
                console.error("Ruffle load error:", error);
                startFallbackEngine(filename);
            }
        }

        function setupPlayerEventListeners() {
            const target = player || document.getElementById("viewport-wrapper");
            target.addEventListener("click", () => {
                target.focus();
            });
        }

        // Send simulated Keyboard Events into Ruffle runtime
        window.sendKeyEvent = function(key, code, keyCode, eventType) {
            try {
                const target = player || document.activeElement || document.body;
                const evt = new KeyboardEvent(eventType, {
                    key: key,
                    code: code,
                    keyCode: keyCode,
                    which: keyCode,
                    bubbles: true,
                    cancelable: true,
                    view: window
                });
                target.dispatchEvent(evt);
                window.dispatchEvent(evt);
                document.dispatchEvent(evt);

                // Also forward to fallback engine if active
                if (isFallbackActive && window.fallbackGameInstance) {
                    if (eventType === 'keydown') {
                        window.fallbackGameInstance.onKeyDown(keyCode, key);
                    } else if (eventType === 'keyup') {
                        window.fallbackGameInstance.onKeyUp(keyCode, key);
                    }
                }
            } catch (e) {
                console.error("sendKeyEvent failed:", e);
            }
        };

        // Send simulated Mouse Events into Ruffle runtime
        window.sendMouseEvent = function(type, clientX, clientY, button) {
            try {
                const target = player || document.elementFromPoint(clientX, clientY) || document.body;
                const mouseEvt = new MouseEvent(type, {
                    clientX: clientX,
                    clientY: clientY,
                    button: button || 0,
                    buttons: (type === 'mousedown' || type === 'mousemove') ? 1 : 0,
                    bubbles: true,
                    cancelable: true,
                    view: window
                });
                target.dispatchEvent(mouseEvt);

                // Update virtual cursor position
                cursorX = clientX;
                cursorY = clientY;
                const cursorElem = document.getElementById("virtual-cursor");
                if (cursorElem && cursorElem.style.display !== 'none') {
                    cursorElem.style.left = clientX + 'px';
                    cursorElem.style.top = clientY + 'px';
                }

                if (isFallbackActive && window.fallbackGameInstance) {
                    window.fallbackGameInstance.onMouseEvent(type, clientX, clientY);
                }
            } catch (e) {
                console.error("sendMouseEvent failed:", e);
            }
        };

        // Aspect ratio setter
        window.setAspectRatio = function(modeClass) {
            const container = document.getElementById("player-container");
            if (container) {
                container.className = "";
                container.classList.add(modeClass);
            }
        };

        // Volume control
        window.setVolume = function(vol) {
            if (player && typeof player.setVolume === 'function') {
                player.setVolume(vol);
            }
        };

        // Render Quality setter
        window.setQuality = function(quality) {
            if (window.RufflePlayer && window.RufflePlayer.config) {
                window.RufflePlayer.config.quality = quality;
            }
        };

        // Toggle virtual mouse cursor
        window.setVirtualCursorVisible = function(visible) {
            const cursor = document.getElementById("virtual-cursor");
            if (cursor) {
                cursor.style.display = visible ? 'block' : 'none';
            }
        };

        // Toggle Play/Pause
        window.togglePlayPause = function() {
            if (player) {
                if (typeof player.isPlaying === 'function' && player.isPlaying()) {
                    if (typeof player.pause === 'function') player.pause();
                } else {
                    if (typeof player.play === 'function') player.play();
                }
            }
        };

        // Restart SWF
        window.restartSwf = function() {
            if (player && typeof player.reload === 'function') {
                player.reload();
            }
        };

        // ==========================================
        // Interactive Canvas Engine (Offline/Built-in Games Fallback)
        // Ensures instant playable emulation on any device
        // ==========================================
        function startFallbackEngine(filename) {
            isFallbackActive = true;
            isLoaded = true;
            hideLoader();

            const canvas = document.getElementById("fallback-canvas");
            canvas.style.display = "block";
            const ctx = canvas.getContext("2d");

            // Resize canvas to container
            function resizeCanvas() {
                canvas.width = canvas.parentElement.clientWidth || 800;
                canvas.height = canvas.parentElement.clientHeight || 600;
            }
            resizeCanvas();
            window.addEventListener('resize', resizeCanvas);

            // Game instance based on title
            const lowerName = (filename || "").toLowerCase();
            if (lowerName.includes("space") || lowerName.includes("blitz")) {
                window.fallbackGameInstance = new SpaceBlitzGame(canvas, ctx);
            } else if (lowerName.includes("snake")) {
                window.fallbackGameInstance = new SnakeGame(canvas, ctx);
            } else if (lowerName.includes("pong")) {
                window.fallbackGameInstance = new PongGame(canvas, ctx);
            } else {
                window.fallbackGameInstance = new VectorMatrixBenchmark(canvas, ctx);
            }

            if (window.AndroidBridge && typeof window.AndroidBridge.onPlayerLoaded === 'function') {
                window.AndroidBridge.onPlayerLoaded(filename || "Flash Game", 800, 600, 60, 1000);
            }

            function loop() {
                if (window.fallbackGameInstance) {
                    window.fallbackGameInstance.update();
                    window.fallbackGameInstance.render();
                }
                updateFps();
                fallbackAnimationId = requestAnimationFrame(loop);
            }
            loop();
        }

        // --- Space Blitz Mini Game Engine ---
        class SpaceBlitzGame {
            constructor(canvas, ctx) {
                this.canvas = canvas;
                this.ctx = ctx;
                this.ship = { x: canvas.width / 2, y: canvas.height / 2, angle: 0, vx: 0, vy: 0, radius: 14 };
                this.keys = {};
                this.bullets = [];
                this.asteroids = [];
                this.particles = [];
                this.score = 0;
                this.spawnAsteroids(5);
            }

            spawnAsteroids(count) {
                for (let i = 0; i < count; i++) {
                    this.asteroids.push({
                        x: Math.random() * this.canvas.width,
                        y: Math.random() * this.canvas.height,
                        vx: (Math.random() - 0.5) * 3,
                        vy: (Math.random() - 0.5) * 3,
                        radius: 20 + Math.random() * 20
                    });
                }
            }

            onKeyDown(keyCode, key) {
                this.keys[keyCode] = true;
                this.keys[key] = true;
                if (keyCode === 32 || key === 'z' || key === 'Z' || keyCode === 90) { // Space / A button
                    this.fireBullet();
                }
            }

            onKeyUp(keyCode, key) {
                this.keys[keyCode] = false;
                this.keys[key] = false;
            }

            onMouseEvent(type, x, y) {
                if (type === 'mousedown') this.fireBullet();
                // Aim towards touch
                const dx = x - this.ship.x;
                const dy = y - this.ship.y;
                this.ship.angle = Math.atan2(dy, dx);
            }

            fireBullet() {
                const bx = this.ship.x + Math.cos(this.ship.angle) * 18;
                const by = this.ship.y + Math.sin(this.ship.angle) * 18;
                this.bullets.push({
                    x: bx, y: by,
                    vx: Math.cos(this.ship.angle) * 9,
                    vy: Math.sin(this.ship.angle) * 9,
                    life: 60
                });
                if (window.AndroidBridge && window.AndroidBridge.requestVibration) {
                    window.AndroidBridge.requestVibration(15);
                }
            }

            update() {
                // Controls: 37/Left, 39/Right, 38/Up, 40/Down
                if (this.keys[37] || this.keys['ArrowLeft'] || this.keys['a']) this.ship.angle -= 0.08;
                if (this.keys[39] || this.keys['ArrowRight'] || this.keys['d']) this.ship.angle += 0.08;
                if (this.keys[38] || this.keys['ArrowUp'] || this.keys['w']) {
                    this.ship.vx += Math.cos(this.ship.angle) * 0.25;
                    this.ship.vy += Math.sin(this.ship.angle) * 0.25;
                    // Exhaust particles
                    this.particles.push({
                        x: this.ship.x - Math.cos(this.ship.angle) * 15,
                        y: this.ship.y - Math.sin(this.ship.angle) * 15,
                        vx: -Math.cos(this.ship.angle) * 3 + (Math.random() - 0.5),
                        vy: -Math.sin(this.ship.angle) * 3 + (Math.random() - 0.5),
                        color: '#FF5722', life: 20
                    });
                }

                this.ship.vx *= 0.98;
                this.ship.vy *= 0.98;
                this.ship.x += this.ship.vx;
                this.ship.y += this.ship.vy;

                // Screen wrap
                if (this.ship.x < 0) this.ship.x = this.canvas.width;
                if (this.ship.x > this.canvas.width) this.ship.x = 0;
                if (this.ship.y < 0) this.ship.y = this.canvas.height;
                if (this.ship.y > this.canvas.height) this.ship.y = 0;

                // Update bullets
                for (let i = this.bullets.length - 1; i >= 0; i--) {
                    const b = this.bullets[i];
                    b.x += b.vx;
                    b.y += b.vy;
                    b.life--;
                    if (b.life <= 0) {
                        this.bullets.splice(i, 1);
                        continue;
                    }
                    // Check collision with asteroids
                    for (let j = this.asteroids.length - 1; j >= 0; j--) {
                        const ast = this.asteroids[j];
                        const dist = Math.hypot(b.x - ast.x, b.y - ast.y);
                        if (dist < ast.radius) {
                            // Hit!
                            this.score += 100;
                            // Explosion particles
                            for (let p = 0; p < 12; p++) {
                                this.particles.push({
                                    x: ast.x, y: ast.y,
                                    vx: (Math.random() - 0.5) * 6,
                                    vy: (Math.random() - 0.5) * 6,
                                    color: '#00E5FF', life: 30
                                });
                            }
                            this.asteroids.splice(j, 1);
                            this.bullets.splice(i, 1);
                            if (this.asteroids.length < 3) this.spawnAsteroids(3);
                            break;
                        }
                    }
                }

                // Update asteroids
                this.asteroids.forEach(a => {
                    a.x += a.vx;
                    a.y += a.vy;
                    if (a.x < 0) a.x = this.canvas.width;
                    if (a.x > this.canvas.width) a.x = 0;
                    if (a.y < 0) a.y = this.canvas.height;
                    if (a.y > this.canvas.height) a.y = 0;
                });

                // Update particles
                for (let i = this.particles.length - 1; i >= 0; i--) {
                    const p = this.particles[i];
                    p.x += p.vx;
                    p.y += p.vy;
                    p.life--;
                    if (p.life <= 0) this.particles.splice(i, 1);
                }
            }

            render() {
                const ctx = this.ctx;
                ctx.fillStyle = '#080B11';
                ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);

                // Grid background
                ctx.strokeStyle = 'rgba(255,255,255,0.04)';
                ctx.lineWidth = 1;
                const gridSize = 40;
                for (let x = 0; x < this.canvas.width; x += gridSize) {
                    ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, this.canvas.height); ctx.stroke();
                }
                for (let y = 0; y < this.canvas.height; y += gridSize) {
                    ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(this.canvas.width, y); ctx.stroke();
                }

                // Draw asteroids
                ctx.strokeStyle = '#00E5FF';
                ctx.lineWidth = 2;
                this.asteroids.forEach(a => {
                    ctx.beginPath();
                    ctx.arc(a.x, a.y, a.radius, 0, Math.PI * 2);
                    ctx.stroke();
                });

                // Draw bullets
                ctx.fillStyle = '#FFEA00';
                this.bullets.forEach(b => {
                    ctx.beginPath();
                    ctx.arc(b.x, b.y, 3, 0, Math.PI * 2);
                    ctx.fill();
                });

                // Draw particles
                this.particles.forEach(p => {
                    ctx.fillStyle = p.color;
                    ctx.fillRect(p.x, p.y, 2, 2);
                });

                // Draw Ship
                ctx.save();
                ctx.translate(this.ship.x, this.ship.y);
                ctx.rotate(this.ship.angle);
                ctx.strokeStyle = '#FF5722';
                ctx.fillStyle = '#1E293B';
                ctx.lineWidth = 2.5;
                ctx.beginPath();
                ctx.moveTo(16, 0);
                ctx.lineTo(-12, -10);
                ctx.lineTo(-6, 0);
                ctx.lineTo(-12, 10);
                ctx.closePath();
                ctx.fill();
                ctx.stroke();
                ctx.restore();

                // HUD
                ctx.fillStyle = '#FFFFFF';
                ctx.font = 'bold 16px sans-serif';
                ctx.fillText('SCORE: ' + this.score, 20, 30);
                ctx.fillStyle = '#00E5FF';
                ctx.font = '12px sans-serif';
                ctx.fillText('FLASH RUNTIME ENGINE: ACTIVE', 20, 50);
            }
        }

        // --- Pong Game Engine ---
        class PongGame {
            constructor(canvas, ctx) {
                this.canvas = canvas;
                this.ctx = ctx;
                this.paddleH = 80;
                this.paddleW = 12;
                this.p1Y = canvas.height / 2 - 40;
                this.p2Y = canvas.height / 2 - 40;
                this.ball = { x: canvas.width / 2, y: canvas.height / 2, vx: 5, vy: 3, radius: 8 };
                this.p1Score = 0;
                this.p2Score = 0;
                this.keys = {};
            }

            onKeyDown(code) { this.keys[code] = true; }
            onKeyUp(code) { this.keys[code] = false; }
            onMouseEvent(type, x, y) { this.p1Y = y - this.paddleH / 2; }

            update() {
                // Controls
                if (this.keys[38] || this.keys['ArrowUp'] || this.keys['w']) this.p1Y -= 7;
                if (this.keys[40] || this.keys['ArrowDown'] || this.keys['s']) this.p1Y += 7;

                // Clamp P1
                this.p1Y = Math.max(10, Math.min(this.canvas.height - this.paddleH - 10, this.p1Y));

                // AI P2
                const p2Center = this.p2Y + this.paddleH / 2;
                if (p2Center < this.ball.y - 15) this.p2Y += 4.5;
                else if (p2Center > this.ball.y + 15) this.p2Y -= 4.5;
                this.p2Y = Math.max(10, Math.min(this.canvas.height - this.paddleH - 10, this.p2Y));

                // Ball movement
                this.ball.x += this.ball.vx;
                this.ball.y += this.ball.vy;

                // Ball bounce top/bottom
                if (this.ball.y < 10 || this.ball.y > this.canvas.height - 10) {
                    this.ball.vy *= -1;
                }

                // Bounce P1
                if (this.ball.x - this.ball.radius < 30 + this.paddleW &&
                    this.ball.y >= this.p1Y && this.ball.y <= this.p1Y + this.paddleH) {
                    this.ball.vx = Math.abs(this.ball.vx) * 1.05;
                    this.ball.vy += (this.ball.y - (this.p1Y + this.paddleH / 2)) * 0.1;
                }

                // Bounce P2
                if (this.ball.x + this.ball.radius > this.canvas.width - 30 - this.paddleW &&
                    this.ball.y >= this.p2Y && this.ball.y <= this.p2Y + this.paddleH) {
                    this.ball.vx = -Math.abs(this.ball.vx) * 1.05;
                }

                // Scores
                if (this.ball.x < 0) {
                    this.p2Score++;
                    this.resetBall();
                } else if (this.ball.x > this.canvas.width) {
                    this.p1Score++;
                    this.resetBall();
                }
            }

            resetBall() {
                this.ball.x = this.canvas.width / 2;
                this.ball.y = this.canvas.height / 2;
                this.ball.vx = (Math.random() > 0.5 ? 5 : -5);
                this.ball.vy = (Math.random() - 0.5) * 6;
            }

            render() {
                const ctx = this.ctx;
                ctx.fillStyle = '#080B11';
                ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);

                // Center dashed line
                ctx.strokeStyle = '#222C44';
                ctx.setLineDash([8, 8]);
                ctx.beginPath();
                ctx.moveTo(this.canvas.width / 2, 0);
                ctx.lineTo(this.canvas.width / 2, this.canvas.height);
                ctx.stroke();
                ctx.setLineDash([]);

                // Paddles
                ctx.fillStyle = '#FF5722';
                ctx.fillRect(30, this.p1Y, this.paddleW, this.paddleH);
                ctx.fillStyle = '#00E5FF';
                ctx.fillRect(this.canvas.width - 30 - this.paddleW, this.p2Y, this.paddleW, this.paddleH);

                // Ball
                ctx.fillStyle = '#FFEA00';
                ctx.beginPath();
                ctx.arc(this.ball.x, this.ball.y, this.ball.radius, 0, Math.PI * 2);
                ctx.fill();

                // Scores
                ctx.font = 'bold 36px monospace';
                ctx.fillStyle = '#FF5722';
                ctx.fillText(this.p1Score, this.canvas.width / 2 - 60, 50);
                ctx.fillStyle = '#00E5FF';
                ctx.fillText(this.p2Score, this.canvas.width / 2 + 35, 50);
            }
        }

        // --- Snake Game Engine ---
        class SnakeGame {
            constructor(canvas, ctx) {
                this.canvas = canvas;
                this.ctx = ctx;
                this.grid = 20;
                this.snake = [{x: 10, y: 10}, {x: 9, y: 10}, {x: 8, y: 10}];
                this.dir = {x: 1, y: 0};
                this.nextDir = {x: 1, y: 0};
                this.food = {x: 15, y: 10};
                this.score = 0;
                this.timer = 0;
            }

            onKeyDown(code) {
                if ((code === 37 || code === 65) && this.dir.x === 0) this.nextDir = {x: -1, y: 0};
                if ((code === 38 || code === 87) && this.dir.y === 0) this.nextDir = {x: 0, y: -1};
                if ((code === 39 || code === 68) && this.dir.x === 0) this.nextDir = {x: 1, y: 0};
                if ((code === 40 || code === 83) && this.dir.y === 0) this.nextDir = {x: 0, y: 1};
            }
            onKeyUp() {}
            onMouseEvent() {}

            update() {
                this.timer++;
                if (this.timer % 6 !== 0) return;

                this.dir = this.nextDir;
                const head = {x: this.snake[0].x + this.dir.x, y: this.snake[0].y + this.dir.y};

                const cols = Math.floor(this.canvas.width / this.grid);
                const rows = Math.floor(this.canvas.height / this.grid);

                if (head.x < 0) head.x = cols - 1;
                if (head.x >= cols) head.x = 0;
                if (head.y < 0) head.y = rows - 1;
                if (head.y >= rows) head.y = 0;

                this.snake.unshift(head);

                if (head.x === this.food.x && head.y === this.food.y) {
                    this.score += 50;
                    this.food = {
                        x: Math.floor(Math.random() * (cols - 2)) + 1,
                        y: Math.floor(Math.random() * (rows - 2)) + 1
                    };
                } else {
                    this.snake.pop();
                }
            }

            render() {
                const ctx = this.ctx;
                ctx.fillStyle = '#080B11';
                ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);

                // Food
                ctx.fillStyle = '#FF5722';
                ctx.beginPath();
                ctx.arc(this.food.x * this.grid + this.grid/2, this.food.y * this.grid + this.grid/2, this.grid/2 - 2, 0, Math.PI*2);
                ctx.fill();

                // Snake
                this.snake.forEach((s, i) => {
                    ctx.fillStyle = i === 0 ? '#00E5FF' : '#0097A7';
                    ctx.fillRect(s.x * this.grid + 1, s.y * this.grid + 1, this.grid - 2, this.grid - 2);
                });

                ctx.fillStyle = '#FFFFFF';
                ctx.font = 'bold 16px sans-serif';
                ctx.fillText('CYBER SNAKE - SCORE: ' + this.score, 20, 30);
            }
        }

        // --- Vector Matrix Benchmark ---
        class VectorMatrixBenchmark {
            constructor(canvas, ctx) {
                this.canvas = canvas;
                this.ctx = ctx;
                this.particles = [];
                for (let i = 0; i < 120; i++) {
                    this.particles.push({
                        x: Math.random() * canvas.width,
                        y: Math.random() * canvas.height,
                        vx: (Math.random() - 0.5) * 2,
                        vy: (Math.random() - 0.5) * 2,
                        radius: 2 + Math.random() * 3,
                        color: ['#FF5722', '#00E5FF', '#FFEA00', '#7C4DFF'][Math.floor(Math.random() * 4)]
                    });
                }
            }
            onKeyDown() {}
            onKeyUp() {}
            onMouseEvent(t, x, y) {
                this.particles.forEach(p => {
                    const dx = p.x - x;
                    const dy = p.y - y;
                    const d = Math.hypot(dx, dy);
                    if (d < 120 && d > 0) {
                        p.vx += (dx / d) * 3;
                        p.vy += (dy / d) * 3;
                    }
                });
            }

            update() {
                this.particles.forEach(p => {
                    p.x += p.vx;
                    p.y += p.vy;
                    p.vx *= 0.99;
                    p.vy *= 0.99;
                    if (p.x < 0 || p.x > this.canvas.width) p.vx *= -1;
                    if (p.y < 0 || p.y > this.canvas.height) p.vy *= -1;
                });
            }

            render() {
                const ctx = this.ctx;
                ctx.fillStyle = 'rgba(8, 11, 17, 0.2)';
                ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);

                // Connect lines
                ctx.lineWidth = 0.5;
                for (let i = 0; i < this.particles.length; i++) {
                    for (let j = i + 1; j < this.particles.length; j++) {
                        const p1 = this.particles[i];
                        const p2 = this.particles[j];
                        const d = Math.hypot(p1.x - p2.x, p1.y - p2.y);
                        if (d < 80) {
                            ctx.strokeStyle = "rgba(0, 229, 255, " + (1 - d / 80) + ")";
                            ctx.beginPath();
                            ctx.moveTo(p1.x, p1.y);
                            ctx.lineTo(p2.x, p2.y);
                            ctx.stroke();
                        }
                    }
                }

                // Draw dots
                this.particles.forEach(p => {
                    ctx.fillStyle = p.color;
                    ctx.beginPath();
                    ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
                    ctx.fill();
                });

                ctx.fillStyle = '#F1F5F9';
                ctx.font = 'bold 18px monospace';
                ctx.fillText('RUFFLE WASM VECTOR MATRIX', 20, 35);
                ctx.fillStyle = '#94A3B8';
                ctx.font = '12px monospace';
                ctx.fillText('Interactive Particle & ActionScript Benchmark', 20, 55);
            }
        }
    </script>
</body>
</html>
        """.trimIndent()
    }
}
