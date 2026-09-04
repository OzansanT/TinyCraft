import * as THREE from './vendor/three.module.js';

const canvas = document.querySelector('#gameCanvas');
const gameFrame = document.querySelector('#gameFrame');
const resetButton = document.querySelector('#resetButton');
const blockButtons = [...document.querySelectorAll('.block-choice')];
const selectedLabel = document.querySelector('#selectedLabel');
const blockCount = document.querySelector('#blockCount');
const positionLabel = document.querySelector('#positionLabel');
const message = document.querySelector('#message');

const renderer = new THREE.WebGLRenderer({ canvas, antialias: true });
renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
renderer.shadowMap.enabled = true;
renderer.shadowMap.type = THREE.PCFSoftShadowMap;

const scene = new THREE.Scene();
scene.background = new THREE.Color(0x8fc7e8);
scene.fog = new THREE.Fog(0x8fc7e8, 14, 34);

const camera = new THREE.PerspectiveCamera(62, 1, 0.1, 100);

const hemisphere = new THREE.HemisphereLight(0xd9f2ff, 0x5c6b3c, 2.2);
scene.add(hemisphere);

const sun = new THREE.DirectionalLight(0xffffff, 2.6);
sun.position.set(8, 16, 5);
sun.castShadow = true;
sun.shadow.mapSize.set(1024, 1024);
sun.shadow.camera.left = -18;
sun.shadow.camera.right = 18;
sun.shadow.camera.top = 18;
sun.shadow.camera.bottom = -18;
scene.add(sun);

const blockGeometry = new THREE.BoxGeometry(1, 1, 1);
const materials = {
  grass: new THREE.MeshLambertMaterial({ color: 0x67a844 }),
  dirt: new THREE.MeshLambertMaterial({ color: 0x805333 }),
  stone: new THREE.MeshLambertMaterial({ color: 0x777b80 }),
  wood: new THREE.MeshLambertMaterial({ color: 0x704522 }),
  leaves: new THREE.MeshLambertMaterial({ color: 0x397842 })
};

const blockGroup = new THREE.Group();
scene.add(blockGroup);

const blocks = new Map();
const raycaster = new THREE.Raycaster();
const pointer = new THREE.Vector2();
const keys = new Set();

const player = new THREE.Mesh(
  new THREE.BoxGeometry(0.55, 1.15, 0.55),
  new THREE.MeshLambertMaterial({ color: 0xf2c94c })
);
player.castShadow = true;
scene.add(player);

const state = {
  selectedBlock: 'grass',
  playerX: 0,
  playerZ: 0,
  yaw: Math.PI * 0.25,
  pitch: 0.55,
  distance: 8,
  dragging: false,
  movedDuringDrag: false,
  pointerStartX: 0,
  pointerStartY: 0,
  pointerDownAt: 0,
  lastX: 0,
  lastY: 0,
  seed: Math.random() * 1000
};

function blockKey(x, y, z) {
  return `${x}|${y}|${z}`;
}

function addBlock(x, y, z, type = 'grass') {
  const key = blockKey(x, y, z);
  if (blocks.has(key) || blocks.size > 1500) return false;

  const mesh = new THREE.Mesh(blockGeometry, materials[type] || materials.grass);
  mesh.position.set(x, y, z);
  mesh.castShadow = true;
  mesh.receiveShadow = true;
  mesh.userData = { x, y, z, type };
  blockGroup.add(mesh);
  blocks.set(key, mesh);
  updateHud();
  return true;
}

function removeBlock(mesh) {
  if (!mesh?.userData) return;
  const { x, y, z } = mesh.userData;
  blockGroup.remove(mesh);
  blocks.delete(blockKey(x, y, z));
  updateHud();
}

function topHeight(x, z) {
  let top = -1;
  for (const mesh of blocks.values()) {
    if (mesh.userData.x === x && mesh.userData.z === z) {
      top = Math.max(top, mesh.userData.y);
    }
  }
  return top;
}

function terrainHeight(x, z) {
  const wave = Math.sin((x + state.seed) * 0.55) + Math.cos((z - state.seed) * 0.48);
  const detail = Math.sin((x + z) * 0.9) * 0.45;
  return Math.max(1, Math.min(4, Math.round(2.2 + wave * 0.55 + detail)));
}

function clearWorld() {
  for (const mesh of [...blocks.values()]) blockGroup.remove(mesh);
  blocks.clear();
}

function generateTree(x, groundY, z) {
  const trunkHeight = 3;
  for (let y = 1; y <= trunkHeight; y += 1) addBlock(x, groundY + y, z, 'wood');

  for (let dx = -1; dx <= 1; dx += 1) {
    for (let dz = -1; dz <= 1; dz += 1) {
      addBlock(x + dx, groundY + trunkHeight, z + dz, 'leaves');
      if (Math.abs(dx) + Math.abs(dz) < 2) {
        addBlock(x + dx, groundY + trunkHeight + 1, z + dz, 'leaves');
      }
    }
  }
}

function generateWorld() {
  clearWorld();
  state.seed = Math.random() * 1000;

  const radius = 7;
  const treeCandidates = [];

  for (let x = -radius; x <= radius; x += 1) {
    for (let z = -radius; z <= radius; z += 1) {
      const height = terrainHeight(x, z);
      for (let y = 0; y <= height; y += 1) {
        let type = 'stone';
        if (y === height) type = 'grass';
        else if (y >= height - 2) type = 'dirt';
        addBlock(x, y, z, type);
      }

      if (Math.abs(x) > 2 && Math.abs(z) > 2 && Math.random() < 0.025) {
        treeCandidates.push({ x, z, y: height });
      }
    }
  }

  treeCandidates.slice(0, 4).forEach(tree => generateTree(tree.x, tree.y, tree.z));

  state.playerX = 0;
  state.playerZ = 0;
  updatePlayerHeight();
  updateCamera();
  updateHud();
  showMessage('New world generated. Click to mine; Shift + click to place.');
}

function updatePlayerHeight() {
  const gridX = Math.round(state.playerX);
  const gridZ = Math.round(state.playerZ);
  const height = topHeight(gridX, gridZ);
  player.position.set(state.playerX, height + 1.08, state.playerZ);
}

function updateCamera() {
  const target = new THREE.Vector3(player.position.x, player.position.y + 0.5, player.position.z);
  const horizontal = Math.cos(state.pitch) * state.distance;

  camera.position.set(
    target.x + Math.sin(state.yaw) * horizontal,
    target.y + Math.sin(state.pitch) * state.distance,
    target.z + Math.cos(state.yaw) * horizontal
  );
  camera.lookAt(target);
}

function updateHud() {
  selectedLabel.textContent = state.selectedBlock[0].toUpperCase() + state.selectedBlock.slice(1);
  blockCount.textContent = String(blocks.size);
  positionLabel.textContent = `${Math.round(state.playerX)}, ${Math.round(state.playerZ)}`;

  blockButtons.forEach(button => {
    button.classList.toggle('active', button.dataset.block === state.selectedBlock);
  });
}

let messageTimer;
function showMessage(text) {
  message.textContent = text;
  clearTimeout(messageTimer);
  messageTimer = setTimeout(() => {
    message.textContent = 'WASD to move · Drag to rotate';
  }, 2400);
}

function selectBlock(type) {
  if (!materials[type]) return;
  state.selectedBlock = type;
  updateHud();
  showMessage(`${type[0].toUpperCase() + type.slice(1)} block selected.`);
}

function resizeRenderer() {
  const width = gameFrame.clientWidth;
  const height = canvas.clientHeight;
  renderer.setSize(width, height, false);
  camera.aspect = width / height;
  camera.updateProjectionMatrix();
}

function getBlockFromPointer(event) {
  const rect = canvas.getBoundingClientRect();
  pointer.x = ((event.clientX - rect.left) / rect.width) * 2 - 1;
  pointer.y = -((event.clientY - rect.top) / rect.height) * 2 + 1;
  raycaster.setFromCamera(pointer, camera);
  const hits = raycaster.intersectObjects(blockGroup.children, false);
  return hits[0] || null;
}

function interactWithBlock(event, forcePlace = false) {
  const hit = getBlockFromPointer(event);
  if (!hit) {
    showMessage('No block selected. Aim at the terrain.');
    return;
  }

  if (event.shiftKey || forcePlace) {
    const normal = hit.face.normal.clone();
    normal.transformDirection(hit.object.matrixWorld).round();
    const position = hit.object.position.clone().add(normal);

    const playerDistance = position.distanceTo(player.position);
    if (playerDistance < 1.1) {
      showMessage('You cannot place a block inside the player.');
      return;
    }

    const placed = addBlock(
      Math.round(position.x),
      Math.round(position.y),
      Math.round(position.z),
      state.selectedBlock
    );
    showMessage(placed ? `${state.selectedBlock} block placed.` : 'That space is occupied.');
  } else {
    if (hit.object.position.y === 0) {
      showMessage('The bottom foundation cannot be mined.');
      return;
    }
    removeBlock(hit.object);
    showMessage('Block mined.');
  }

  updatePlayerHeight();
  updateCamera();
}

function handleMovement(delta) {
  const speed = 3.25 * delta;
  let forward = 0;
  let sideways = 0;

  if (keys.has('KeyW') || keys.has('ArrowUp')) forward += 1;
  if (keys.has('KeyS') || keys.has('ArrowDown')) forward -= 1;
  if (keys.has('KeyA') || keys.has('ArrowLeft')) sideways -= 1;
  if (keys.has('KeyD') || keys.has('ArrowRight')) sideways += 1;

  if (!forward && !sideways) return;

  const length = Math.hypot(forward, sideways) || 1;
  forward /= length;
  sideways /= length;

  const forwardX = -Math.sin(state.yaw);
  const forwardZ = -Math.cos(state.yaw);
  const rightX = Math.cos(state.yaw);
  const rightZ = -Math.sin(state.yaw);

  const nextX = state.playerX + (forwardX * forward + rightX * sideways) * speed;
  const nextZ = state.playerZ + (forwardZ * forward + rightZ * sideways) * speed;

  if (Math.abs(nextX) <= 6.8 && Math.abs(nextZ) <= 6.8) {
    const currentHeight = topHeight(Math.round(state.playerX), Math.round(state.playerZ));
    const nextHeight = topHeight(Math.round(nextX), Math.round(nextZ));

    if (nextHeight >= 0 && nextHeight - currentHeight <= 1) {
      state.playerX = nextX;
      state.playerZ = nextZ;
      updatePlayerHeight();
      updateHud();
    }
  }
}

blockButtons.forEach(button => {
  button.addEventListener('click', () => selectBlock(button.dataset.block));
});

resetButton.addEventListener('click', generateWorld);

window.addEventListener('keydown', event => {
  if (['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight', 'Space'].includes(event.code)) {
    event.preventDefault();
  }
  keys.add(event.code);
  if (event.code === 'Digit1') selectBlock('grass');
  if (event.code === 'Digit2') selectBlock('dirt');
  if (event.code === 'Digit3') selectBlock('stone');
});

window.addEventListener('keyup', event => keys.delete(event.code));

canvas.addEventListener('pointerdown', event => {
  state.dragging = true;
  state.movedDuringDrag = false;
  state.pointerStartX = event.clientX;
  state.pointerStartY = event.clientY;
  state.pointerDownAt = performance.now();
  state.lastX = event.clientX;
  state.lastY = event.clientY;
  canvas.classList.add('dragging');
  canvas.setPointerCapture(event.pointerId);
});

canvas.addEventListener('pointermove', event => {
  if (!state.dragging) return;

  const totalDistance = Math.hypot(
    event.clientX - state.pointerStartX,
    event.clientY - state.pointerStartY
  );
  if (totalDistance > 5) state.movedDuringDrag = true;

  if (state.movedDuringDrag) {
    const dx = event.clientX - state.lastX;
    const dy = event.clientY - state.lastY;
    state.yaw -= dx * 0.008;
    state.pitch = THREE.MathUtils.clamp(state.pitch + dy * 0.006, 0.2, 1.25);
    updateCamera();
  }

  state.lastX = event.clientX;
  state.lastY = event.clientY;
});

canvas.addEventListener('pointerup', event => {
  if (!state.movedDuringDrag) {
    const longPress = performance.now() - state.pointerDownAt >= 450;
    interactWithBlock(event, longPress && event.pointerType !== 'mouse');
  }

  state.dragging = false;
  canvas.classList.remove('dragging');
  if (canvas.hasPointerCapture(event.pointerId)) canvas.releasePointerCapture(event.pointerId);
});

canvas.addEventListener('pointercancel', () => {
  state.dragging = false;
  canvas.classList.remove('dragging');
});

canvas.addEventListener('contextmenu', event => event.preventDefault());

canvas.addEventListener('wheel', event => {
  event.preventDefault();
  state.distance = THREE.MathUtils.clamp(state.distance + event.deltaY * 0.008, 4.5, 13);
  updateCamera();
}, { passive: false });

const resizeObserver = new ResizeObserver(resizeRenderer);
resizeObserver.observe(gameFrame);

let previousTime = performance.now();
function animate(currentTime) {
  const delta = Math.min((currentTime - previousTime) / 1000, 0.05);
  previousTime = currentTime;
  handleMovement(delta);
  updateCamera();
  renderer.render(scene, camera);
  requestAnimationFrame(animate);
}

resizeRenderer();
generateWorld();
requestAnimationFrame(animate);
