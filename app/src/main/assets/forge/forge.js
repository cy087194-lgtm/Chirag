// Forge Reactor 3D WebGL Engine
(function() {
    let scene, camera, renderer;
    let coreSphere, wireIcosahedron, rings = [];
    let particles, floorGrid;
    let stageNodes = [];
    let animationFrameId;
    let isDisposed = false;
    let theme = {
        background: 0x06050E,
        accent: 0xFFB547,
        lagoon: 0x3FE0D0,
        lotus: 0xFF6FAE
    };

    function init(themeConfig) {
        if (themeConfig) {
            try {
                const parsed = typeof themeConfig === 'string' ? JSON.parse(themeConfig) : themeConfig;
                if (parsed.background) theme.background = parseInt(parsed.background.replace('#', '0x'));
                if (parsed.accent) theme.accent = parseInt(parsed.accent.replace('#', '0x'));
                if (parsed.lagoon) theme.lagoon = parseInt(parsed.lagoon.replace('#', '0x'));
                if (parsed.lotus) theme.lotus = parseInt(parsed.lotus.replace('#', '0x'));
            } catch(e) {}
        }

        const container = document.getElementById('canvas-container');
        const width = container.clientWidth || window.innerWidth;
        const height = container.clientHeight || window.innerHeight;

        scene = new THREE.Scene();
        scene.background = new THREE.Color(theme.background);
        scene.fog = new THREE.FogExp2(theme.background, 0.035);

        camera = new THREE.PerspectiveCamera(60, width / height, 0.1, 1000);
        camera.position.set(0, 4, 12);
        camera.lookAt(0, 0, 0);

        try {
            renderer = new THREE.WebGLRenderer({ antialias: true, alpha: false });
            renderer.setSize(width, height);
            renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2.0));
            container.appendChild(renderer.domElement);
        } catch(err) {
            if (window.ForgeBridge && window.ForgeBridge.onWebGLFailed) {
                window.ForgeBridge.onWebGLFailed();
            }
            return;
        }

        // Ambient & Directional Lights
        const ambientLight = new THREE.AmbientLight(0xffffff, 0.6);
        scene.add(ambientLight);
        const pointLight = new THREE.PointLight(theme.accent, 2.0, 50);
        pointLight.position.set(0, 2, 5);
        scene.add(pointLight);

        buildReactorCore();
        buildFloorGrid();
        buildParticles();
        buildStageNodes();

        animate();

        if (window.ForgeBridge && window.ForgeBridge.onReady) {
            window.ForgeBridge.onReady();
        }
    }

    function buildReactorCore() {
        const coreGeo = new THREE.SphereGeometry(1.4, 32, 32);
        const coreMat = new THREE.MeshStandardMaterial({
            color: theme.lagoon,
            emissive: theme.lagoon,
            emissiveIntensity: 0.6,
            roughness: 0.2,
            metalness: 0.8
        });
        coreSphere = new THREE.Mesh(coreGeo, coreMat);
        scene.add(coreSphere);

        const wireGeo = new THREE.IcosahedronGeometry(1.9, 1);
        const wireMat = new THREE.MeshBasicMaterial({
            color: theme.accent,
            wireframe: true,
            transparent: true,
            opacity: 0.45
        });
        wireIcosahedron = new THREE.Mesh(wireGeo, wireMat);
        scene.add(wireIcosahedron);

        // 3 Gyro Torus Rings
        for (let i = 0; i < 3; i++) {
            const ringGeo = new THREE.TorusGeometry(2.4 + (i * 0.4), 0.04, 16, 64);
            const ringMat = new THREE.MeshBasicMaterial({
                color: i === 0 ? theme.accent : (i === 1 ? theme.lotus : theme.lagoon),
                transparent: true,
                opacity: 0.7
            });
            const ring = new THREE.Mesh(ringGeo, ringMat);
            ring.rotation.x = i * Math.PI / 3;
            ring.rotation.y = i * Math.PI / 4;
            rings.push(ring);
            scene.add(ring);
        }
    }

    function buildFloorGrid() {
        floorGrid = new THREE.GridHelper(30, 30, theme.accent, 0x1E1B4B);
        floorGrid.position.y = -3.5;
        scene.add(floorGrid);
    }

    function buildParticles() {
        const count = 900;
        const geo = new THREE.BufferGeometry();
        const positions = new Float32Array(count * 3);

        for (let i = 0; i < count; i++) {
            const radius = 3 + Math.random() * 8;
            const theta = Math.random() * Math.PI * 2;
            const y = (Math.random() - 0.5) * 8;

            positions[i * 3] = radius * Math.cos(theta);
            positions[i * 3 + 1] = y;
            positions[i * 3 + 2] = radius * Math.sin(theta);
        }

        geo.setAttribute('position', new THREE.BufferAttribute(positions, 3));
        const mat = new THREE.PointsMaterial({
            color: theme.accent,
            size: 0.12,
            transparent: true,
            opacity: 0.75
        });

        particles = new THREE.Points(geo, mat);
        scene.add(particles);
    }

    function buildStageNodes() {
        const radius = 5.0;
        for (let i = 0; i < 6; i++) {
            const angle = (i / 6) * Math.PI * 2;
            const nodeGeo = new THREE.SphereGeometry(0.3, 16, 16);
            const nodeMat = new THREE.MeshStandardMaterial({
                color: 0x332F5C,
                emissive: 0x221E4D,
                roughness: 0.3
            });
            const node = new THREE.Mesh(nodeGeo, nodeMat);
            node.position.set(radius * Math.cos(angle), -1.5, radius * Math.sin(angle));
            stageNodes.push(node);
            scene.add(node);
        }
    }

    function updateProgress(progress, stageIndex) {
        if (!stageNodes.length) return;
        for (let i = 0; i < stageNodes.length; i++) {
            if (i <= stageIndex) {
                stageNodes[i].material.color.setHex(theme.accent);
                stageNodes[i].material.emissive.setHex(theme.accent);
                stageNodes[i].scale.set(1.4, 1.4, 1.4);
            } else {
                stageNodes[i].material.color.setHex(0x332F5C);
                stageNodes[i].material.emissive.setHex(0x221E4D);
                stageNodes[i].scale.set(1.0, 1.0, 1.0);
            }
        }
    }

    function reveal() {
        let startTime = performance.now();
        function runReveal(time) {
            const elapsed = (time - startTime) / 1000;
            if (elapsed < 1.5) {
                camera.position.z -= 0.08;
                camera.fov -= 0.15;
                camera.updateProjectionMatrix();
                requestAnimationFrame(runReveal);
            } else {
                if (window.ForgeBridge && window.ForgeBridge.onRevealFinished) {
                    window.ForgeBridge.onRevealFinished();
                }
            }
        }
        requestAnimationFrame(runReveal);
    }

    function animate(timestamp) {
        if (isDisposed) return;
        animationFrameId = requestAnimationFrame(animate);

        const time = timestamp ? timestamp * 0.001 : 0;

        if (coreSphere) {
            coreSphere.rotation.y = time * 0.4;
        }
        if (wireIcosahedron) {
            wireIcosahedron.rotation.x = -time * 0.3;
            wireIcosahedron.rotation.y = time * 0.25;
        }

        rings.forEach((ring, idx) => {
            ring.rotation.x += 0.01 * (idx + 1);
            ring.rotation.y += 0.015 * (idx + 1);
        });

        if (particles) {
            particles.rotation.y = time * 0.08;
        }

        renderer.render(scene, camera);
    }

    function dispose() {
        isDisposed = true;
        cancelAnimationFrame(animationFrameId);
        if (renderer && renderer.domElement && renderer.domElement.parentNode) {
            renderer.domElement.parentNode.removeChild(renderer.domElement);
        }
        if (renderer) renderer.dispose();
    }

    window.Forge = {
        init: init,
        update: function(stateJson) {
            try {
                const s = typeof stateJson === 'string' ? JSON.parse(stateJson) : stateJson;
                if (s.progress !== undefined && s.stageIndex !== undefined) {
                    updateProgress(s.progress, s.stageIndex);
                }
            } catch(e) {}
        },
        reveal: reveal,
        dispose: dispose
    };
})();
