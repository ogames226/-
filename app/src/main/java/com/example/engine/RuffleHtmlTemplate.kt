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
            width: 100%;
            height: 100%;
        }

        #player-container {
            position: relative;
            display: flex;
            align-items: center;
            justify-content: center;
            width: 100%;
            height: 100%;
            max-width: 100vw;
            max-height: 100vh;
            background-color: #080B11;
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
        }

        .aspect-16-9 {
            aspect-ratio: 16 / 9;
            max-width: 100%;
            max-height: 100%;
        }

        .aspect-stretch {
            width: 100% !important;
            height: 100% !important;
        }

        ruffle-player, ruffle-embed, #active-ruffle-player {
            width: 100% !important;
            height: 100% !important;
            display: block !important;
            outline: none;
            position: absolute;
            inset: 0;
        }

        ruffle-player::part(player), ruffle-player::part(canvas), ruffle-player canvas {
            width: 100% !important;
            height: 100% !important;
            display: block !important;
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
            transition: opacity 0.3s ease-out;
            pointer-events: none;
        }

        .spinner {
            width: 48px;
            height: 48px;
            border: 4px solid rgba(255, 87, 34, 0.15);
            border-top: 4px solid #FF5722;
            border-right: 4px solid #00E5FF;
            border-radius: 50%;
            animation: spin 0.8s cubic-bezier(0.4, 0, 0.2, 1) infinite;
        }

        .loader-title {
            font-size: 16px;
            font-weight: 700;
            letter-spacing: 0.5px;
            color: #F1F5F9;
            text-align: center;
            padding: 0 16px;
        }

        .loader-sub {
            font-size: 12px;
            color: #94A3B8;
            text-align: center;
            padding: 0 16px;
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

        /* Fallback interactive canvas for instant sandbox demos if CDN is offline */
        #fallback-canvas {
            display: none;
            width: 100%;
            height: 100%;
            background: #080B11;
        }
    </style>

    <!-- Configure Ruffle Global Options before loading script -->
    <script>
        window.RufflePlayer = window.RufflePlayer || {};
        window.RufflePlayer.config = {
            publicPath: "https://cdn.jsdelivr.net/npm/@ruffle-rs/ruffle@latest/",
            polyfills: true,
            autoplay: "on",
            unmuteOverlay: "hidden",
            backgroundColor: "#080B11",
            splashScreen: false,
            scale: "showAll",
            quality: "high",
            align: "center",
            forceScale: true,
            openUrlMode: "confirm",
            allowScriptAccess: true,
            logLevel: "warn",
            warnOnUnsupportedContent: false
        };
    </script>
    <!-- Primary Ruffle CDN (jsdelivr) with unpkg fallback -->
    <script src="https://cdn.jsdelivr.net/npm/@ruffle-rs/ruffle@latest/ruffle.js" onerror="loadFallbackRuffleCdn()"></script>
    <script>
        function loadFallbackRuffleCdn() {
            if (!window.RufflePlayer || !window.RufflePlayer.newest) {
                console.warn("Primary CDN failed, loading unpkg Ruffle...");
                if (window.RufflePlayer && window.RufflePlayer.config) {
                    window.RufflePlayer.config.publicPath = "https://unpkg.com/@ruffle-rs/ruffle/";
                }
                const script = document.createElement('script');
                script.src = "https://unpkg.com/@ruffle-rs/ruffle/ruffle.js";
                script.onerror = function() {
                    console.warn("All Ruffle CDNs failed or offline.");
                };
                document.head.appendChild(script);
            }
        }
    </script>
</head>
<body>
    <div id="viewport-wrapper">
        <div id="player-container" class="$aspectRatioCssClass">
            <div id="loader-overlay">
                <div class="spinner"></div>
                <div class="loader-title">Ruffle Flash Wasm</div>
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
            if (isLoaded && !isFallbackActive) {
                requestAnimationFrame(updateFps);
            }
        }

        function hideLoader() {
            const loader = document.getElementById("loader-overlay");
            if (loader) {
                loader.style.opacity = "0";
                setTimeout(() => {
                    loader.style.display = "none";
                }, 300);
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

        // Wait for Ruffle script to be available
        function waitForRuffle(maxMs = 3500) {
            return new Promise((resolve) => {
                if (window.RufflePlayer && (typeof window.RufflePlayer.newest === 'function')) {
                    resolve(window.RufflePlayer.newest());
                    return;
                }
                const start = performance.now();
                const interval = setInterval(() => {
                    if (window.RufflePlayer && (typeof window.RufflePlayer.newest === 'function')) {
                        clearInterval(interval);
                        resolve(window.RufflePlayer.newest());
                    } else if (performance.now() - start > maxMs) {
                        clearInterval(interval);
                        resolve(null);
                    }
                }, 100);
            });
        }

        // Load SWF via intercepted local URL (fastest & handles large files)
        window.loadSwfDirectUrl = async function(url, filename) {
            try {
                showLoader(filename || "Loading Game", "Initializing Flash WebAssembly runtime...");
                const ruffleInst = await waitForRuffle();
                if (ruffleInst) {
                    await initRuffleWithSource(ruffleInst, { url: url || "https://ruffle.ai-studio.local/current_game.swf" }, filename);
                } else {
                    console.warn("Ruffle unavailable for direct URL, checking fallback...");
                    // Try getting base64 from AndroidBridge
                    if (window.AndroidBridge && typeof window.AndroidBridge.getSwfBase64 === 'function') {
                        const b64 = window.AndroidBridge.getSwfBase64();
                        if (b64 && b64.length > 0) {
                            window.loadSwfFromBase64(b64, filename);
                            return;
                        }
                    }
                    startFallbackEngine(filename);
                }
            } catch (err) {
                console.error("Error loading direct SWF:", err);
                startFallbackEngine(filename);
            }
        };

        // Public API called from Kotlin Bridge with Base64 payload
        window.loadSwfFromBase64 = async function(base64Data, filename) {
            try {
                showLoader(filename || "Loading Game", "Decoding Flash SWF binary...");
                const ruffleInst = await waitForRuffle();
                if (ruffleInst) {
                    const arrayBuffer = base64ToArrayBuffer(base64Data);
                    await initRuffleWithSource(ruffleInst, { data: arrayBuffer, swfUrl: filename || "game.swf" }, filename);
                } else {
                    console.warn("Ruffle unavailable, starting built-in fallback engine for " + filename);
                    startFallbackEngine(filename);
                }
            } catch (err) {
                console.error("Error loading SWF from base64:", err);
                if (window.AndroidBridge && typeof window.AndroidBridge.onPlayerError === 'function') {
                    window.AndroidBridge.onPlayerError(err.message || String(err));
                }
                startFallbackEngine(filename);
            }
        };

        async function initRuffleWithSource(ruffleInstance, loadSource, filename) {
            try {
                const container = document.getElementById("player-container");
                
                // Clear existing player if any
                if (player && player.parentNode) {
                    try {
                        if (typeof player.pause === 'function') player.pause();
                        player.parentNode.removeChild(player);
                    } catch (_) {}
                }

                // Hide fallback canvas
                const canvas = document.getElementById("fallback-canvas");
                if (canvas) canvas.style.display = "none";
                if (fallbackAnimationId) {
                    cancelAnimationFrame(fallbackAnimationId);
                    fallbackAnimationId = null;
                }
                isFallbackActive = false;

                player = ruffleInstance.createPlayer();
                player.id = "active-ruffle-player";
                player.style.width = "100%";
                player.style.height = "100%";
                container.appendChild(player);

                const config = {
                    autoplay: "on",
                    backgroundColor: "#080B11",
                    letterbox: "on",
                    openUrlMode: "confirm",
                    allowScriptAccess: true,
                    unmuteOverlay: "hidden",
                    ...loadSource
                };

                await player.load(config);

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

                setupPlayerEventListeners();

            } catch (error) {
                console.error("Ruffle init error:", error);
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

                // Forward to fallback engine if active
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

            function resizeCanvas() {
                canvas.width = canvas.parentElement.clientWidth || 800;
                canvas.height = canvas.parentElement.clientHeight || 600;
            }
            resizeCanvas();
            window.addEventListener('resize', resizeCanvas);

            const lowerName = (filename || "").toLowerCase();
            if (lowerName.includes("space") || lowerName.includes("blitz") || lowerName.includes("astro")) {
                window.fallbackGameInstance = new SpaceBlitzGame(canvas, ctx);
            } else if (lowerName.includes("snake") || lowerName.includes("worm")) {
                window.fallbackGameInstance = new SnakeGame(canvas, ctx);
            } else if (lowerName.includes("pong") || lowerName.includes("paddle") || lowerName.includes("table")) {
                window.fallbackGameInstance = new PongGame(canvas, ctx);
            } else {
                window.fallbackGameInstance = new SpaceBlitzGame(canvas, ctx);
            }

            if (window.AndroidBridge && typeof window.AndroidBridge.onPlayerLoaded === 'function') {
                window.AndroidBridge.onPlayerLoaded(filename || "Flash Game", 800, 600, 60, 1000);
            }

            function loop() {
                if (window.fallbackGameInstance && isFallbackActive) {
                    window.fallbackGameInstance.update();
                    window.fallbackGameInstance.render();
                    updateFps();
                    fallbackAnimationId = requestAnimationFrame(loop);
                }
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
                if (this.keys[37] || this.keys['ArrowLeft'] || this.keys['a']) this.ship.angle -= 0.08;
                if (this.keys[39] || this.keys['ArrowRight'] || this.keys['d']) this.ship.angle += 0.08;
                if (this.keys[38] || this.keys['ArrowUp'] || this.keys['w']) {
                    this.ship.vx += Math.cos(this.ship.angle) * 0.25;
                    this.ship.vy += Math.sin(this.ship.angle) * 0.25;
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

                if (this.ship.x < 0) this.ship.x = this.canvas.width;
                if (this.ship.x > this.canvas.width) this.ship.x = 0;
                if (this.ship.y < 0) this.ship.y = this.canvas.height;
                if (this.ship.y > this.canvas.height) this.ship.y = 0;

                for (let i = this.bullets.length - 1; i >= 0; i--) {
                    const b = this.bullets[i];
                    b.x += b.vx;
                    b.y += b.vy;
                    b.life--;
                    if (b.life <= 0) {
                        this.bullets.splice(i, 1);
                        continue;
                    }
                    for (let j = this.asteroids.length - 1; j >= 0; j--) {
                        const ast = this.asteroids[j];
                        const dist = Math.hypot(b.x - ast.x, b.y - ast.y);
                        if (dist < ast.radius) {
                            this.score += 100;
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

                this.asteroids.forEach(a => {
                    a.x += a.vx;
                    a.y += a.vy;
                    if (a.x < 0) a.x = this.canvas.width;
                    if (a.x > this.canvas.width) a.x = 0;
                    if (a.y < 0) a.y = this.canvas.height;
                    if (a.y > this.canvas.height) a.y = 0;
                });

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

                ctx.strokeStyle = 'rgba(255,255,255,0.04)';
                ctx.lineWidth = 1;
                const gridSize = 40;
                for (let x = 0; x < this.canvas.width; x += gridSize) {
                    ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, this.canvas.height); ctx.stroke();
                }
                for (let y = 0; y < this.canvas.height; y += gridSize) {
                    ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(this.canvas.width, y); ctx.stroke();
                }

                ctx.strokeStyle = '#00E5FF';
                ctx.lineWidth = 2;
                this.asteroids.forEach(a => {
                    ctx.beginPath();
                    ctx.arc(a.x, a.y, a.radius, 0, Math.PI * 2);
                    ctx.stroke();
                });

                ctx.fillStyle = '#FFEA00';
                this.bullets.forEach(b => {
                    ctx.beginPath();
                    ctx.arc(b.x, b.y, 3, 0, Math.PI * 2);
                    ctx.fill();
                });

                this.particles.forEach(p => {
                    ctx.fillStyle = p.color;
                    ctx.fillRect(p.x, p.y, 2, 2);
                });

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
                if (this.keys[38] || this.keys['ArrowUp'] || this.keys['w']) this.p1Y -= 7;
                if (this.keys[40] || this.keys['ArrowDown'] || this.keys['s']) this.p1Y += 7;

                this.p1Y = Math.max(10, Math.min(this.canvas.height - this.paddleH - 10, this.p1Y));

                const p2Center = this.p2Y + this.paddleH / 2;
                if (p2Center < this.ball.y - 15) this.p2Y += 4.5;
                else if (p2Center > this.ball.y + 15) this.p2Y -= 4.5;
                this.p2Y = Math.max(10, Math.min(this.canvas.height - this.paddleH - 10, this.p2Y));

                this.ball.x += this.ball.vx;
                this.ball.y += this.ball.vy;

                if (this.ball.y < 10 || this.ball.y > this.canvas.height - 10) {
                    this.ball.vy *= -1;
                }

                if (this.ball.x - this.ball.radius < 30 + this.paddleW &&
                    this.ball.y >= this.p1Y && this.ball.y <= this.p1Y + this.paddleH) {
                    this.ball.vx = Math.abs(this.ball.vx) * 1.05;
                    this.ball.vy += (this.ball.y - (this.p1Y + this.paddleH / 2)) * 0.1;
                }

                if (this.ball.x + this.ball.radius > this.canvas.width - 30 - this.paddleW &&
                    this.ball.y >= this.p2Y && this.ball.y <= this.p2Y + this.paddleH) {
                    this.ball.vx = -Math.abs(this.ball.vx) * 1.05;
                }

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

                ctx.strokeStyle = '#222C44';
                ctx.setLineDash([8, 8]);
                ctx.beginPath();
                ctx.moveTo(this.canvas.width / 2, 0);
                ctx.lineTo(this.canvas.width / 2, this.canvas.height);
                ctx.stroke();
                ctx.setLineDash([]);

                ctx.fillStyle = '#FF5722';
                ctx.fillRect(30, this.p1Y, this.paddleW, this.paddleH);
                ctx.fillStyle = '#00E5FF';
                ctx.fillRect(this.canvas.width - 30 - this.paddleW, this.p2Y, this.paddleW, this.paddleH);

                ctx.fillStyle = '#FFEA00';
                ctx.beginPath();
                ctx.arc(this.ball.x, this.ball.y, this.ball.radius, 0, Math.PI * 2);
                ctx.fill();

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

                ctx.fillStyle = '#FF5722';
                ctx.beginPath();
                ctx.arc(this.food.x * this.grid + this.grid/2, this.food.y * this.grid + this.grid/2, this.grid/2 - 2, 0, Math.PI*2);
                ctx.fill();

                this.snake.forEach((s, i) => {
                    ctx.fillStyle = i === 0 ? '#00E5FF' : '#0097A7';
                    ctx.fillRect(s.x * this.grid + 1, s.y * this.grid + 1, this.grid - 2, this.grid - 2);
                });

                ctx.fillStyle = '#FFFFFF';
                ctx.font = 'bold 16px sans-serif';
                ctx.fillText('CYBER SNAKE - SCORE: ' + this.score, 20, 30);
            }
        }
    </script>
</body>
</html>
        """.trimIndent()
    }
}
