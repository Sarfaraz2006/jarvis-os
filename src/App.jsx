import React, { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { CapacitorFlash } from '@capgo/capacitor-flash';
import { AppLauncher } from '@capacitor/app-launcher';
import { Settings as SettingsIcon, Volume2, VolumeX, Radio, Zap, Shield, Heart } from 'lucide-react';
import './App.css';

// Audio Context helper
let audioContextInstance = null;
let humOsc = null;
let humGain = null;

export default function App() {
  // Main state flags
  const [booted, setBooted] = useState(false);
  const [bootProgress, setBootProgress] = useState(0);
  const [bootStatusText, setBootStatusText] = useState("Calibrating Stark OS Interface...");
  
  const [theme, setTheme] = useState('jarvis'); // jarvis, friday, safety, redalert
  const [sfxEnabled, setSfxEnabled] = useState(true);
  const [voiceEnabled, setVoiceEnabled] = useState(true);
  const [isListening, setIsListening] = useState(false);
  const [systemPower, setSystemPower] = useState(100);
  
  // Power sliders
  const [sliders, setSliders] = useState({
    thrusters: 90,
    repulsors: 75,
    shields: 80,
    lifesupport: 95
  });

  // Diagnostics panel status
  const [diagPart, setDiagPart] = useState({
    title: "SYSTEM STATUS",
    details: [
      { name: "Optic HUD Sensors", val: "ONLINE [100%]", status: "ok" },
      { name: "Main Arc Interface", val: "STABLE [100%]", status: "ok" },
      { name: "Weapon Controls", val: "STANDBY [100%]", status: "ok" },
      { name: "Thruster Arrays", val: "OPERATIONAL [94%]", status: "ok" }
    ]
  });

  // Chat message console
  const [chatLogs, setChatLogs] = useState([
    {
      sender: "J.A.R.V.I.S.",
      text: "Uplink established. All systems nominal, sir. I have generated a diagnostics map and initialized Stark UI. How can I help you today?",
      isUser: false
    }
  ]);

  // Telemetry log feeds
  const [telemetryLogs, setTelemetryLogs] = useState([
    { text: "Secure connection initiated on terminal.", type: "info", time: new Date().toLocaleTimeString() },
    { text: "Background Stark HUD loaded successfully.", type: "info", time: new Date().toLocaleTimeString() },
    { text: "Main Arc core temperature normal.", type: "info", time: new Date().toLocaleTimeString() }
  ]);

  // API Key & Settings
  const [showSettings, setShowSettings] = useState(false);
  const [apiKey, setApiKey] = useState(() => {
    return localStorage.getItem('STARK_GEMINI_KEY') || '';
  });
  const [tempKey, setTempKey] = useState('');

  // DOM and Web API Refs
  const chatEndRef = useRef(null);
  const telemetryEndRef = useRef(null);
  const canvasRef = useRef(null);
  const recognitionRef = useRef(null);
  const currentThemeRef = useRef(theme);
  const systemPowerRef = useRef(systemPower);

  // Sync refs to avoid stale closures in listeners
  useEffect(() => {
    currentThemeRef.current = theme;
  }, [theme]);

  useEffect(() => {
    systemPowerRef.current = systemPower;
  }, [systemPower]);

  // System telemetry helper
  const addTelemetryLog = (text, type = "info") => {
    const time = new Date().toLocaleTimeString();
    setTelemetryLogs(prev => [...prev, { text, type, time }]);
  };

  // Text to Speech
  const speak = (text) => {
    if (!voiceEnabled) return;
    try {
      window.speechSynthesis.cancel();
      const utterance = new SpeechSynthesisUtterance(text);
      const voices = window.speechSynthesis.getVoices();
      
      let preferredVoice;
      if (currentThemeRef.current === 'friday') {
        // FRIDAY: US Female / high frequency
        preferredVoice = voices.find(v => v.lang.includes('en-US') && (v.name.toLowerCase().includes('female') || v.name.toLowerCase().includes('zira')));
      } else {
        // JARVIS: British Male / Daniel
        preferredVoice = voices.find(v => v.lang.includes('en-GB') || v.name.toLowerCase().includes('daniel') || v.name.toLowerCase().includes('google uk'));
      }

      if (preferredVoice) utterance.voice = preferredVoice;
      utterance.rate = currentThemeRef.current === 'friday' ? 1.1 : 1.02;
      utterance.pitch = currentThemeRef.current === 'friday' ? 1.05 : 0.92;
      window.speechSynthesis.speak(utterance);
    } catch (e) {
      console.error("Text to speech failed", e);
    }
  };

  // Initialize Web Audio Synth Hum
  const initAudio = () => {
    if (audioContextInstance) return;
    try {
      audioContextInstance = new (window.AudioContext || window.webkitAudioContext)();
      humOsc = audioContextInstance.createOscillator();
      humGain = audioContextInstance.createGain();
      
      humOsc.type = 'triangle';
      humOsc.frequency.setValueAtTime(55, audioContextInstance.currentTime);
      humGain.gain.setValueAtTime(0.012, audioContextInstance.currentTime);
      
      humOsc.connect(humGain);
      humGain.connect(audioContextInstance.destination);
      humOsc.start();
      addTelemetryLog("Core Synthesizer hum initialized at 55Hz.", "info");
    } catch (e) {
      console.error("Failed to initialize Audio Context", e);
    }
  };

  // Play Beep sound
  const playBeep = (freq = 800, type = 'sine', duration = 0.08, vol = 0.04) => {
    if (!sfxEnabled || !audioContextInstance) return;
    try {
      const osc = audioContextInstance.createOscillator();
      const gain = audioContextInstance.createGain();
      
      osc.type = type;
      osc.frequency.setValueAtTime(freq, audioContextInstance.currentTime);
      gain.gain.setValueAtTime(vol, audioContextInstance.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.0001, audioContextInstance.currentTime + duration);
      
      osc.connect(gain);
      gain.connect(audioContextInstance.destination);
      
      osc.start();
      osc.stop(audioContextInstance.currentTime + duration);
    } catch (e) {
      console.error(e);
    }
  };

  // Play sweeps (e.g. repulsors, overload)
  const playSweep = (startFreq = 200, endFreq = 800, duration = 0.5, type = 'sine', vol = 0.05) => {
    if (!sfxEnabled || !audioContextInstance) return;
    try {
      const osc = audioContextInstance.createOscillator();
      const gain = audioContextInstance.createGain();
      
      osc.type = type;
      osc.frequency.setValueAtTime(startFreq, audioContextInstance.currentTime);
      osc.frequency.exponentialRampToValueAtTime(endFreq, audioContextInstance.currentTime + duration);
      
      gain.gain.setValueAtTime(vol, audioContextInstance.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.0001, audioContextInstance.currentTime + duration);
      
      osc.connect(gain);
      gain.connect(audioContextInstance.destination);
      
      osc.start();
      osc.stop(audioContextInstance.currentTime + duration);
    } catch (e) {
      console.error(e);
    }
  };

  // Red Alert alarm sound loop
  const triggerAlarmSound = () => {
    if (!sfxEnabled || !audioContextInstance) return;
    let count = 0;
    const interval = setInterval(() => {
      if (currentThemeRef.current !== 'redalert' || count > 4) {
        clearInterval(interval);
        return;
      }
      playBeep(880, 'sawtooth', 0.2, 0.02);
      setTimeout(() => playBeep(660, 'sawtooth', 0.2, 0.02), 150);
      count++;
    }, 600);
  };

  // Manage Background hum volume
  useEffect(() => {
    if (humGain && audioContextInstance) {
      if (sfxEnabled) {
        humGain.gain.setValueAtTime(0.012, audioContextInstance.currentTime);
      } else {
        humGain.gain.setValueAtTime(0, audioContextInstance.currentTime);
      }
    }
  }, [sfxEnabled]);

  // Manage Hum frequency by power state
  useEffect(() => {
    if (humOsc && audioContextInstance) {
      let freq = 55;
      if (systemPower === 200) freq = 110;
      else if (systemPower === 25) freq = 30;
      humOsc.frequency.setValueAtTime(freq, audioContextInstance.currentTime);
    }
  }, [systemPower]);

  // Adjust theme variables
  useEffect(() => {
    const root = document.documentElement;
    root.className = ''; // Reset
    if (theme === 'redalert') {
      root.classList.add('red-alert-active');
    } else if (theme === 'friday') {
      root.classList.add('orange-theme-active');
    } else if (theme === 'safety') {
      root.classList.add('green-theme-active');
    }
  }, [theme]);

  // Auto-scroll chat & logs
  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [chatLogs]);

  useEffect(() => {
    telemetryEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [telemetryLogs]);

  // Booting Sequence simulation
  useEffect(() => {
    if (booted) return;
    const interval = setInterval(() => {
      setBootProgress(prev => {
        const next = prev + Math.floor(Math.random() * 8) + 2;
        if (next >= 100) {
          clearInterval(interval);
          setBootStatusText("Uplink Secure. Ready for operator initialization.");
          return 100;
        }
        
        // Progress text feedback
        if (next > 20 && next < 45) {
          setBootStatusText("Calibrating Neural Interface...");
        } else if (next >= 45 && next < 75) {
          setBootStatusText("Loading Arc Reactor Telemetry...");
        } else if (next >= 75 && next < 95) {
          setBootStatusText("Synchronizing J.A.R.V.I.S. Core Matrix...");
        }
        return next;
      });
    }, 70);

    return () => clearInterval(interval);
  }, [booted]);

  const handleBootComplete = () => {
    initAudio();
    playSweep(150, 1000, 1.2, 'sawtooth', 0.05);
    setBooted(true);
    speak("Welcome back, sir. I have initialized the Stark HUD interface. Standing by.");
  };

  // Hologram Canvas rotation drawing
  useEffect(() => {
    if (!booted || !canvasRef.current) return;
    
    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');
    let animationId = null;
    let rotationAngle = 0;
    let hologramMode = 'wireframe-suit'; // wireframe-suit, radar-pulse, cube

    const handleCanvasClick = () => {
      playBeep(1100, 'sine', 0.05, 0.03);
      if (hologramMode === 'wireframe-suit') {
        hologramMode = 'radar-pulse';
        document.getElementById('hologram-mode-txt').textContent = "RADAR SCAN TELEMETRY";
      } else if (hologramMode === 'radar-pulse') {
        hologramMode = 'cube';
        document.getElementById('hologram-mode-txt').textContent = "TESSERACT CUBE PROJ";
      } else {
        hologramMode = 'wireframe-suit';
        document.getElementById('hologram-mode-txt').textContent = "3D WIREFRAME HELMET";
      }
    };
    canvas.addEventListener('click', handleCanvasClick);

    const fitCanvas = () => {
      if (canvas.parentElement) {
        canvas.width = canvas.parentElement.clientWidth;
        canvas.height = canvas.parentElement.clientHeight - 40;
      }
    };
    window.addEventListener('resize', fitCanvas);
    fitCanvas();

    const draw = () => {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      
      // Determine stroke color by active theme
      const t = currentThemeRef.current;
      ctx.strokeStyle = t === 'redalert' ? 'rgba(255, 0, 80, 0.45)' : 
                        t === 'friday' ? 'rgba(255, 120, 0, 0.45)' : 
                        t === 'safety' ? 'rgba(0, 255, 120, 0.45)' : 'rgba(0, 243, 255, 0.45)';
      ctx.lineWidth = 1;
      
      const cx = canvas.width / 2;
      const cy = canvas.height / 2;
      rotationAngle += 0.01;

      if (hologramMode === 'wireframe-suit') {
        // Draw rotating 3D iron man helmet representation
        ctx.beginPath();
        for (let i = 0; i < 360; i += 15) {
          const rad = (i * Math.PI) / 180 + rotationAngle;
          const r = 50 + Math.sin(rad * 4) * 8;
          const x = cx + Math.cos(rad) * r;
          const y = cy + Math.sin(rad * 2.2) * 50 - 10;
          if (i === 0) ctx.moveTo(x, y);
          else ctx.lineTo(x, y);
        }
        ctx.closePath();
        ctx.stroke();

        // Eye slits glow
        ctx.fillStyle = t === 'redalert' ? 'rgba(255, 0, 80, 0.2)' : 
                        t === 'friday' ? 'rgba(255, 120, 0, 0.2)' : 
                        t === 'safety' ? 'rgba(0, 255, 120, 0.2)' : 'rgba(0, 243, 255, 0.2)';
        ctx.beginPath();
        ctx.ellipse(cx - 15, cy - 10, 8, 3, rotationAngle * 0.5, 0, Math.PI * 2);
        ctx.ellipse(cx + 15, cy - 10, 8, 3, -rotationAngle * 0.5, 0, Math.PI * 2);
        ctx.fill();
        ctx.stroke();

        // Crosshairs alignment
        ctx.beginPath();
        ctx.moveTo(cx - 70, cy); ctx.lineTo(cx - 50, cy);
        ctx.moveTo(cx + 50, cy); ctx.lineTo(cx + 70, cy);
        ctx.moveTo(cx, cy - 70); ctx.lineTo(cx, cy - 50);
        ctx.moveTo(cx, cy + 50); ctx.lineTo(cx, cy + 70);
        ctx.stroke();

        ctx.beginPath();
        ctx.arc(cx, cy, 65, 0, Math.PI * 2);
        ctx.stroke();
      } else if (hologramMode === 'radar-pulse') {
        // Sweep radar circles
        ctx.beginPath();
        ctx.arc(cx, cy, 70, 0, Math.PI * 2);
        ctx.stroke();
        
        ctx.beginPath();
        ctx.arc(cx, cy, 40, 0, Math.PI * 2);
        ctx.stroke();
        
        // Swiping hand
        const sx = cx + Math.cos(rotationAngle * 1.5) * 70;
        const sy = cy + Math.sin(rotationAngle * 1.5) * 70;
        ctx.beginPath();
        ctx.moveTo(cx, cy);
        ctx.lineTo(sx, sy);
        ctx.stroke();
      } else if (hologramMode === 'cube') {
        // Rotating 3D wireframe cube
        const size = 35;
        const points = [
          {x: -1, y: -1, z: -1}, {x: 1, y: -1, z: -1}, {x: 1, y: 1, z: -1}, {x: -1, y: 1, z: -1},
          {x: -1, y: -1, z: 1},  {x: 1, y: -1, z: 1},  {x: 1, y: 1, z: 1},  {x: -1, y: 1, z: 1}
        ];
        
        const proj = points.map(p => {
          let y1 = p.y * Math.cos(rotationAngle) - p.z * Math.sin(rotationAngle);
          let z1 = p.y * Math.sin(rotationAngle) + p.z * Math.cos(rotationAngle);
          let x2 = p.x * Math.cos(rotationAngle) + z1 * Math.sin(rotationAngle);
          let z2 = -p.x * Math.sin(rotationAngle) + z1 * Math.cos(rotationAngle);
          
          const scale = 150 / (150 + z2 * size);
          return {
            x: cx + x2 * size * scale,
            y: cy + y1 * size * scale
          };
        });
        
        const drawLine = (i, j) => {
          ctx.beginPath();
          ctx.moveTo(proj[i].x, proj[i].y);
          ctx.lineTo(proj[j].x, proj[j].y);
          ctx.stroke();
        };
        
        for (let i = 0; i < 4; i++) {
          drawLine(i, (i + 1) % 4);
          drawLine(i + 4, ((i + 1) % 4) + 4);
          drawLine(i, i + 4);
        }
      }

      animationId = requestAnimationFrame(draw);
    };
    draw();

    return () => {
      cancelAnimationFrame(animationId);
      canvas.removeEventListener('click', handleCanvasClick);
      window.removeEventListener('resize', fitCanvas);
    };
  }, [booted]);

  // Speech Recognition API setup
  useEffect(() => {
    if (!booted) return;
    
    window.SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (window.SpeechRecognition) {
      const rec = new window.SpeechRecognition();
      rec.continuous = false;
      rec.lang = 'en-US';
      rec.interimResults = false;

      rec.onresult = (event) => {
        const text = event.results[0][0].transcript;
        addTelemetryLog(`Voice decoded: "${text}"`, "info");
        handleUserTextSubmit(text);
      };

      rec.onerror = (err) => {
        console.error("Speech Recognition error:", err);
        setIsListening(false);
      };

      rec.onend = () => {
        setIsListening(false);
      };

      recognitionRef.current = rec;
    } else {
      addTelemetryLog("Speech Recognition API not supported in this browser environment.", "warn");
    }
  }, [booted]);

  const toggleMicListening = () => {
    playBeep(600);
    if (!recognitionRef.current) {
      addTelemetryLog("No microphone service active.", "warn");
      return;
    }

    if (isListening) {
      recognitionRef.current.stop();
      setIsListening(false);
    } else {
      try {
        recognitionRef.current.start();
        setIsListening(true);
        addTelemetryLog("Voice listener activated.", "info");
      } catch (e) {
        console.error(e);
      }
    }
  };

  // Capacitor-based device actions
  const actionFlashlight = async (turnOn) => {
    try {
      if (turnOn) {
        const isAvail = await CapacitorFlash.isAvailable();
        if (isAvail?.value) {
          await CapacitorFlash.switchOn({ intensity: 1.0 });
          addTelemetryLog("Capacitor Native Flashlight set to ON.", "info");
          return "System flashlight initialized, sir.";
        }
      } else {
        await CapacitorFlash.switchOff();
        addTelemetryLog("Capacitor Native Flashlight set to OFF.", "info");
        return "Flashlight deactivated, sir.";
      }
    } catch (e) {
      console.warn("Flashlight API fallback", e);
    }
    // Fallback simulation text
    addTelemetryLog(`Simulating Flashlight toggle: ${turnOn ? 'ON' : 'OFF'}`, "warn");
    return `Flashlight ${turnOn ? 'activated' : 'deactivated'} (Simulation Mode).`;
  };

  const actionLaunchApp = async (appName) => {
    const urls = {
      youtube: 'https://youtube.com',
      spotify: 'https://open.spotify.com',
      maps: 'https://maps.google.com',
      chrome: 'https://google.com',
      whatsapp: 'https://web.whatsapp.com'
    };
    
    const targetUrl = urls[appName.toLowerCase()] || `https://www.google.com/search?q=${encodeURIComponent(appName)}`;
    try {
      await AppLauncher.openUrl({ url: targetUrl });
      addTelemetryLog(`Launching native app redirection for: ${appName}`, "info");
      return `Initializing uplink redirect to ${appName}, sir.`;
    } catch (e) {
      console.warn("Capacitor AppLauncher failed, falling back to browser window", e);
    }
    window.open(targetUrl, '_blank');
    addTelemetryLog(`Simulating App Launch: ${appName} (Opened in new browser tab)`, "warn");
    return `Opening ${appName} in browser tab, sir.`;
  };

  // Central Action Matcher & Executor (triggered by Gemini function calls or Local parsing)
  const executeSystemAction = async (actionName, params = {}) => {
    playBeep(700, 'sine', 0.1, 0.05);
    let logMsg = "";
    let responseText = "";

    switch (actionName) {
      case 'turn_on_flashlight':
        responseText = await actionFlashlight(true);
        break;
      case 'turn_off_flashlight':
        responseText = await actionFlashlight(false);
        break;
      case 'open_app':
        responseText = await actionLaunchApp(params.appName || 'Google');
        break;
      case 'change_theme':
        const nextTheme = params.theme?.toLowerCase();
        if (['jarvis', 'friday', 'safety', 'redalert'].includes(nextTheme)) {
          setTheme(nextTheme);
          logMsg = `Main shell interface switched to: ${nextTheme.toUpperCase()}`;
          responseText = nextTheme === 'friday' ? 
            "Switching user-interface shell to Friday load backup matrix. Hello sir!" : 
            nextTheme === 'redalert' ? "Activating red alert protocol. Weapon arrays are charged." :
            nextTheme === 'safety' ? "Switching interface to safety diagnostics console." :
            "Reloading standard Jarvis mainframe interface, sir.";
          
          if (nextTheme === 'redalert') {
            setSystemPower(200);
            triggerAlarmSound();
          } else if (nextTheme === 'safety') {
            setSystemPower(25);
          } else {
            setSystemPower(100);
          }
        }
        break;
      case 'adjust_power_level':
        const nextPower = Number(params.level);
        if (!isNaN(nextPower)) {
          setSystemPower(nextPower);
          logMsg = `Arc Core capacity configured to ${nextPower}%.`;
          responseText = `Arc Core capacity configured to ${nextPower} percent, sir.`;
          if (nextPower >= 200) {
            setTheme('redalert');
            triggerAlarmSound();
          } else if (nextPower <= 25) {
            setTheme('safety');
          } else {
            setTheme('jarvis');
          }
        }
        break;
      case 'trigger_protocol':
        const protocol = params.protocol?.toLowerCase();
        if (protocol === 'diagnostics scan') {
          responseText = "Initializing diagnostic sweep. Analyzing micro-capacitors and optical arrays. One moment, sir.";
          addTelemetryLog("Diagnostic sweep initiated.", "info");
          
          // Animate schematic components sequentially
          let index = 0;
          const parts = ["part-helmet", "part-neck", "part-chest", "part-left-arm", "part-right-arm", "part-left-leg", "part-right-leg"];
          const diagInterval = setInterval(() => {
            if (index >= parts.length) {
              clearInterval(diagInterval);
              setChatLogs(prev => [...prev, {
                sender: currentThemeRef.current === 'friday' ? 'F.R.I.D.A.Y.' : 'J.A.R.V.I.S.',
                text: "System diagnostics completed. All subsystems are functioning within design limits. Weapon systems armed on standby. Arc Core: Stable.",
                isUser: false
              }]);
              speak("System diagnostics completed. All subsystems are functioning within design limits.");
              return;
            }
            const el = document.getElementById(parts[index]);
            if (el) {
              el.style.fill = 'rgba(0, 243, 255, 0.4)';
              playBeep(400 + index * 100, 'sine', 0.05, 0.05);
              addTelemetryLog(`Node scanned: ${parts[index].replace('part-', '').toUpperCase()} - PASS`, "info");
              setTimeout(() => { el.style.fill = 'none'; }, 400);
            }
            index++;
          }, 300);
        } else if (protocol === 'repulsor test') {
          responseText = "Charging repulsor nodes. Testing magnetic confinement rings. Standby for plasma discharge simulation.";
          addTelemetryLog("Repulsors firing system test requested.", "warn");
          
          setTimeout(() => {
            const lArm = document.getElementById('part-left-arm');
            const rArm = document.getElementById('part-right-arm');
            if (lArm) lArm.style.fill = 'rgba(255, 255, 255, 0.8)';
            if (rArm) rArm.style.fill = 'rgba(255, 255, 255, 0.8)';
            playSweep(200, 1500, 0.8, 'sine', 0.06);
            addTelemetryLog("Plasma capacitor output calibrated: 100%.", "info");
          }, 1200);

          setTimeout(() => {
            const lArm = document.getElementById('part-left-arm');
            const rArm = document.getElementById('part-right-arm');
            if (lArm) lArm.style.fill = 'none';
            if (rArm) rArm.style.fill = 'none';
            setChatLogs(prev => [...prev, {
              sender: currentThemeRef.current === 'friday' ? 'F.R.I.D.A.Y.' : 'J.A.R.V.I.S.',
              text: "Repulsor diagnostics show fully operational state, sir. Repulsor beam focusing lenses are clean.",
              isUser: false
            }]);
            speak("Repulsor diagnostics show fully operational state, sir.");
          }, 2200);
        } else if (protocol === 'house party') {
          responseText = "House Party Protocol initialized. Deploying local auxiliary armor suits Mark 42, 47, and 78, sir.";
          addTelemetryLog("HOUSE PARTY PROTOCOL ENGAGED. Deploying 12 suits...", "warn");
          let counter = 1;
          const houseInterval = setInterval(() => {
            if (counter > 4) {
              clearInterval(houseInterval);
              addTelemetryLog("Auxiliary flight arrays deployed and orbiting Stark Tower.", "info");
              return;
            }
            addTelemetryLog(`Suit Mk-${30 + counter} deployed and airborne.`, "info");
            playSweep(250 * counter, 550 * counter, 0.4, 'triangle', 0.04);
            counter++;
          }, 500);
        } else if (protocol === 'clean slate') {
          responseText = "Clean Slate Protocol activated. Initializing thermite self-destruction on all local auxiliary suits in 3 seconds.";
          addTelemetryLog("CLEAN SLATE PROTOCOL DEPLOYED - SELF-DESTRUCTING SECURE CORES", "crit");
          playSweep(800, 50, 2.0, 'sawtooth', 0.08);
          
          setTimeout(() => {
            setSystemPower(0);
            setTheme('safety');
            const parts = document.querySelectorAll('.suit-part');
            parts.forEach(p => p.style.stroke = 'var(--red-bright)');
            addTelemetryLog("All combat suits destroyed. Arc core deactivated.", "crit");
            setChatLogs(prev => [...prev, {
              sender: currentThemeRef.current === 'friday' ? 'F.R.I.D.A.Y.' : 'J.A.R.V.I.S.',
              text: "All local suits destroyed. Security quarantine active.",
              isUser: false
            }]);
            speak("All auxiliary suits have been destroyed.");
          }, 3000);
        }
        break;
      default:
        break;
    }

    if (logMsg) addTelemetryLog(logMsg, "info");
    return responseText;
  };

  // Local rule-based offline NLP matching (when Gemini Key is not configured)
  const processLocalTextCommand = async (text) => {
    const cmd = text.toLowerCase();
    
    // Quick match triggers
    if (cmd.includes('help') || cmd.includes('protocol')) {
      const resText = "Protocols: 'diagnostics scan', 'repulsor test', 'red alert', 'house party', 'clean slate', 'power level [25/100/200]', 'change system to friday'.";
      speak(resText);
      return resText;
    } 
    else if (cmd.includes('diagnostics') || cmd.includes('scan') || cmd.includes('status')) {
      return await executeSystemAction('trigger_protocol', { protocol: 'diagnostics scan' });
    }
    else if (cmd.includes('repulsor test') || cmd.includes('repulsors')) {
      return await executeSystemAction('trigger_protocol', { protocol: 'repulsor test' });
    }
    else if (cmd.includes('red alert') || cmd.includes('combat') || cmd.includes('danger')) {
      await executeSystemAction('change_theme', { theme: 'redalert' });
      return "Red alert command console engaged. Shields set to maximum capacity.";
    }
    else if (cmd.includes('flashlight') || cmd.includes('torch')) {
      const turnOn = cmd.includes('on') || cmd.includes('chalu') || cmd.includes('jalo');
      return await executeSystemAction(turnOn ? 'turn_on_flashlight' : 'turn_off_flashlight');
    }
    else if (cmd.includes('open') || cmd.includes('chalaye') || cmd.includes('chalao')) {
      // Find app name
      let app = "Chrome";
      if (cmd.includes('youtube')) app = "YouTube";
      else if (cmd.includes('spotify')) app = "Spotify";
      else if (cmd.includes('maps') || cmd.includes('map')) app = "Maps";
      else if (cmd.includes('whatsapp')) app = "WhatsApp";
      return await executeSystemAction('open_app', { appName: app });
    }
    else if (cmd.includes('power level') || cmd.includes('capacity')) {
      let val = 100;
      if (cmd.includes('200')) val = 200;
      else if (cmd.includes('25') || cmd.includes('low')) val = 25;
      return await executeSystemAction('adjust_power_level', { level: val });
    }
    else if (cmd.includes('friday') || cmd.includes('theme friday')) {
      await executeSystemAction('change_theme', { theme: 'friday' });
      return "Friday system shell interface active.";
    }
    else if (cmd.includes('jarvis') || cmd.includes('theme jarvis')) {
      await executeSystemAction('change_theme', { theme: 'jarvis' });
      return "Jarvis standard core matrix loaded.";
    }
    else if (cmd.includes('house party')) {
      return await executeSystemAction('trigger_protocol', { protocol: 'house party' });
    }
    else if (cmd.includes('clean slate')) {
      return await executeSystemAction('trigger_protocol', { protocol: 'clean slate' });
    }

    // Default witty AI responses
    const responses = [
      "I have processed your query, sir. However, Stark network firewalls limit my ability to execute that action directly without a Gemini connection.",
      "Indeed, sir. Might I suggest setting up the Google AI Studio Gemini API Key in Settings to enable full intelligence?",
      "Tony would likely advise against that, but as I am programmed to assist, I will log the request.",
      "Fascinating inquiry. I am monitoring multiple encrypted frequencies for further information.",
      "Telemetry signals indicate normal levels. I remain at your service."
    ];
    const randomRes = responses[Math.floor(Math.random() * responses.length)];
    speak(randomRes);
    return randomRes;
  };

  // Google AI Studio Gemini API content generator
  const getGeminiResponse = async (userText) => {
    if (!apiKey) {
      addTelemetryLog("Gemini API Key missing. Falling back to local offline mode.", "warn");
      return await processLocalTextCommand(userText);
    }

    addTelemetryLog("Sending telemetry query to Gemini API...", "info");
    
    // Construct system instructions
    const systemPrompt = `You are ${theme === 'friday' ? 'FRIDAY' : 'JARVIS'}, Tony Stark's advanced artificial intelligence system assistant. 
    Keep your answers concise, intelligent, dry, and polite. 
    If the theme is JARVIS, speak with British accents, and call the user 'Sir'.
    If the theme is FRIDAY, act energetic and call the user 'Boss' or 'Sir'.
    Always respond directly to the user's intent. 
    
    You have direct control of the mobile device capabilities and HUD user interface. If the user asks you to perform an action (e.g., toggle flashlight/torch, open an app, trigger red alert/safety/diagnostics scan, adjust power levels), you MUST call the matching tool/function immediately.`;

    const requestPayload = {
      contents: [
        {
          role: "user",
          parts: [{ text: userText }]
        }
      ],
      systemInstruction: {
        parts: [{ text: systemPrompt }]
      },
      tools: [
        {
          functionDeclarations: [
            {
              name: "turn_on_flashlight",
              description: "Turns on the device camera flashlight / torch."
            },
            {
              name: "turn_off_flashlight",
              description: "Turns off the device camera flashlight / torch."
            },
            {
              name: "open_app",
              description: "Opens a mobile app on the phone by its name.",
              parameters: {
                type: "OBJECT",
                properties: {
                  appName: {
                    type: "STRING",
                    description: "The name of the app to launch (e.g. YouTube, Spotify, Chrome, Maps, WhatsApp)."
                  }
                },
                required: ["appName"]
              }
            },
            {
              name: "change_theme",
              description: "Changes the HUD UI theme color scheme.",
              parameters: {
                type: "OBJECT",
                properties: {
                  theme: {
                    type: "STRING",
                    description: "The theme name. Allowed values: 'jarvis', 'friday', 'safety', 'redalert'."
                  }
                },
                required: ["theme"]
              }
            },
            {
              name: "adjust_power_level",
              description: "Configures the Arc Core system power level percentage.",
              parameters: {
                type: "OBJECT",
                properties: {
                  level: {
                    type: "NUMBER",
                    description: "The power level percentage (e.g., 25, 100, 200)."
                  }
                },
                required: ["level"]
              }
            },
            {
              name: "trigger_protocol",
              description: "Triggers specific suit defensive or testing protocols.",
              parameters: {
                type: "OBJECT",
                properties: {
                  protocol: {
                    type: "STRING",
                    description: "The protocol to trigger. Allowed values: 'diagnostics scan', 'repulsor test', 'house party', 'clean slate'."
                  }
                },
                required: ["protocol"]
              }
            }
          ]
        }
      ]
    };

    try {
      const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${apiKey}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestPayload)
      });

      if (!response.ok) {
        throw new Error(`HTTP Error: ${response.status}`);
      }

      const data = await response.json();
      const candidate = data.candidates?.[0];
      const part = candidate?.content?.parts?.[0];

      if (part?.functionCall) {
        const { name, args } = part.functionCall;
        addTelemetryLog(`AI invoked function call: ${name}()`, "info");
        const actionResult = await executeSystemAction(name, args);
        speak(actionResult);
        return actionResult;
      } else if (part?.text) {
        const textResult = part.text;
        speak(textResult);
        return textResult;
      }

      return "I processed your request, sir. Telemetry is stable.";
    } catch (e) {
      console.error("Gemini API request failed:", e);
      addTelemetryLog(`Gemini request failed: ${e.message}`, "crit");
      return await processLocalTextCommand(userText);
    }
  };

  const handleUserTextSubmit = async (text) => {
    if (!text.trim()) return;
    
    // Add user message to UI
    setChatLogs(prev => [...prev, { sender: "Operator", text, isUser: true }]);
    playBeep(700, 'sine', 0.08, 0.04);
    
    // Call Gemini or local engine
    const reply = await getGeminiResponse(text);
    
    // Add AI message to UI
    setChatLogs(prev => [...prev, { 
      sender: currentThemeRef.current === 'friday' ? "F.R.I.D.A.Y." : "J.A.R.V.I.S.", 
      text: reply, 
      isUser: false 
    }]);
  };

  // Suit Part Diagnostics Poller onClick handlers
  const handleSuitPartClick = (partId) => {
    playBeep(900, 'sine', 0.05, 0.05);
    const partsMap = {
      'part-helmet': {
        title: "Helmet Optic System",
        details: [
          { name: "Optic HUD Sensors", val: "ONLINE [100%]", status: "ok" },
          { name: "HUD Eye Gaskets", val: "SECURE [100%]", status: "ok" },
          { name: "Target Tracker", val: "STANDBY", status: "ok" },
          { name: "Atmospheric PSI", val: "1.0 atm", status: "ok" }
        ]
      },
      'part-neck': {
        title: "Cervical Stabilization",
        details: [
          { name: "Neck Actuators", val: "ONLINE [100%]", status: "ok" },
          { name: "Stabilization", val: "ACTIVE", status: "ok" },
          { name: "Thermal Dissipator", val: "NORMAL [34°C]", status: "ok" }
        ]
      },
      'part-chest': {
        title: "Arc Core Housing Interface",
        details: [
          { name: "Power Line", val: "CONNECTED", status: "ok" },
          { name: "Energy Grid Flow", val: "99.8%", status: "ok" },
          { name: "Shield Matrix Gen", val: "ONLINE", status: "ok" },
          { name: "Aux Power Capacitors", val: "CHARGED [100%]", status: "ok" }
        ]
      },
      'part-left-arm': {
        title: "Left Repulsor Arm",
        details: [
          { name: "Repulsor Capacitor", val: "CHARGED [100%]", status: "ok" },
          { name: "Missile Launcher", val: "RE-ARMING", status: "warning" },
          { name: "Laser Array Matrix", val: "OPERATIONAL", status: "ok" }
        ]
      },
      'part-right-arm': {
        title: "Right Repulsor Arm",
        details: [
          { name: "Repulsor Capacitor", val: "CHARGED [100%]", status: "ok" },
          { name: "Micro-Munition Cell", val: "ACTIVE", status: "ok" },
          { name: "Nanotech Armor", val: "REPAIRING [100%]", status: "ok" }
        ]
      },
      'part-left-leg': {
        title: "Left Jet Stabilizer",
        details: [
          { name: "Thruster Output", val: "94% OUTPUT", status: "ok" },
          { name: "Vectoring Nozzle", val: "ACTIVE", status: "ok" },
          { name: "Thermal Core Temp", val: "42°C", status: "ok" }
        ]
      },
      'part-right-leg': {
        title: "Right Jet Stabilizer",
        details: [
          { name: "Thruster Output", val: "94% OUTPUT", status: "ok" },
          { name: "Vectoring Nozzle", val: "ACTIVE", status: "ok" },
          { name: "Thermal Core Temp", val: "41°C", status: "ok" }
        ]
      }
    };

    const detailsObj = partsMap[partId];
    if (detailsObj) {
      setDiagPart(detailsObj);
      addTelemetryLog(`Diagnostics polled for node: ${detailsObj.title}`, "info");
    }
  };

  // Adjust reactor power manually by clicking it
  const handleArcReactorClick = () => {
    const curPower = systemPower;
    if (curPower === 100) {
      executeSystemAction('adjust_power_level', { level: 200 });
      speak("Arc Reactor output boosted to two hundred percent, sir. Extreme warning is advised.");
      playSweep(440, 1200, 1.0, 'sawtooth', 0.08);
    } else if (curPower === 200) {
      executeSystemAction('adjust_power_level', { level: 25 });
      speak("Arc core overload avoided. Entering low energy reserve mode, sir.");
      playSweep(800, 100, 1.0, 'sine', 0.06);
    } else {
      executeSystemAction('adjust_power_level', { level: 100 });
      speak("Arc core stabilized. Normal power restored.");
      playSweep(100, 500, 0.5, 'sine', 0.05);
    }
  };

  // Handle slide controls
  const handleSliderChange = (name, val) => {
    setSliders(prev => ({ ...prev, [name]: val }));
    playBeep(300 + val * 4, 'sine', 0.03, 0.015);
    addTelemetryLog(`Power allocation: ${name.charAt(0).toUpperCase() + name.slice(1)} adjusted to ${val}%.`, "info");
  };

  // Save Settings Gemini Key
  const handleSaveSettings = () => {
    localStorage.setItem('STARK_GEMINI_KEY', tempKey);
    setApiKey(tempKey);
    setShowSettings(false);
    playBeep(900, 'sine', 0.15, 0.06);
    addTelemetryLog("Security credentials updated in local secure storage.", "info");
    speak("Credentials updated, sir.");
  };

  const handleOpenSettings = () => {
    setTempKey(apiKey);
    setShowSettings(true);
    playBeep(600);
  };

  return (
    <div className="jarvis-body">
      {/* Background filters */}
      <svg style={{ display: 'none' }}>
        <defs>
          <filter id="glow-filter" x="-20%" y="-20%" width="140%" height="140%">
            <feGaussianBlur stdDeviation="5" result="blur" />
            <feMerge>
              <feMergeNode in="blur" />
              <feMergeNode in="SourceGraphic" />
            </feMerge>
          </filter>
          <filter id="glow-filter-heavy" x="-40%" y="-40%" width="180%" height="180%">
            <feGaussianBlur stdDeviation="8" result="blur1" />
            <feGaussianBlur stdDeviation="3" result="blur2" />
            <feMerge>
              <feMergeNode in="blur1" />
              <feMergeNode in="blur2" />
              <feMergeNode in="SourceGraphic" />
            </feMerge>
          </filter>
        </defs>
      </svg>

      {/* Booting Loader */}
      <AnimatePresence>
        {!booted && (
          <motion.div 
            id="boot-overlay"
            exit={{ opacity: 0 }}
            transition={{ duration: 1.0 }}
          >
            <div className="boot-content">
              <div className="boot-logo">STARK OS v9.64</div>
              <div className="boot-status">{bootStatusText}</div>
              <div className="boot-progress">
                <div 
                  className="boot-progress-bar" 
                  style={{ width: `${bootProgress}%` }}
                ></div>
              </div>
              {bootProgress >= 100 && (
                <button className="boot-btn" onClick={handleBootComplete}>
                  INITIALIZE J.A.R.V.I.S.
                </button>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Settings Modal */}
      {showSettings && (
        <div className="settings-overlay">
          <div className="settings-card">
            <h3>Secure Uplink Settings</h3>
            <label>Google Gemini API Key</label>
            <input 
              type="password" 
              value={tempKey} 
              onChange={(e) => setTempKey(e.target.value)} 
              placeholder="Paste your Gemini API Key here..."
            />
            <div className="settings-help">
              This app uses the Gemini 1.5 Flash API as Jarvis's brain. You can generate a free API Key from Google AI Studio. The key is saved locally in your phone storage and is never uploaded anywhere.
            </div>
            <div className="settings-actions">
              <button className="settings-btn save" onClick={handleSaveSettings}>SAVE KEY</button>
              <button className="settings-btn cancel" onClick={() => setShowSettings(false)}>CANCEL</button>
            </div>
          </div>
        </div>
      )}

      {/* Header */}
      {booted && (
        <>
          <header>
            <div className="logo-container">
              <span className="stark-logo">STARK</span>
              <span style={{ color: 'rgba(255,255,255,0.3)', fontWeight: 300 }}>|</span>
              <span className="os-label">
                {theme === 'friday' ? 'F.R.I.D.A.Y.' : theme === 'redalert' ? 'RED ALERT CONSOLE' : theme === 'safety' ? 'SAFETY MODE' : 'J.A.R.V.I.S. OS'}
              </span>
            </div>
            
            <div className="hud-stats-header">
              <div className="stat-item">
                <span className="stat-label">Power Core</span>
                <span className="stat-value">{systemPower}%</span>
              </div>
              <div className="stat-item">
                <span className="stat-label">Connection</span>
                <span className="stat-value" style={{ color: 'var(--green-bright)' }}>SECURE</span>
              </div>
              <div className="stat-item">
                <span className="stat-label">Uplink Code</span>
                <span className="stat-value" style={{ color: apiKey ? 'var(--green-bright)' : 'var(--orange-bright)' }}>
                  {apiKey ? 'API CONNECTED' : 'OFFLINE MODE'}
                </span>
              </div>
            </div>

            <div className="controls-container">
              <button 
                className={`hud-btn ${voiceEnabled ? 'active' : ''}`} 
                onClick={() => {
                  playBeep(400);
                  setVoiceEnabled(!voiceEnabled);
                  addTelemetryLog(`Voice output toggled ${!voiceEnabled ? 'ON' : 'OFF'}.`);
                }}
              >
                <Volume2 size={13} /> {voiceEnabled ? 'VOICE ON' : 'VOICE OFF'}
              </button>
              <button 
                className={`hud-btn ${isListening ? 'active' : ''}`}
                onClick={toggleMicListening}
              >
                <Radio size={13} className={isListening ? 'animate-pulse' : ''} /> {isListening ? 'LISTENING...' : 'MIC OFF'}
              </button>
              <button 
                className={`hud-btn ${sfxEnabled ? 'active' : ''}`}
                onClick={() => {
                  setSfxEnabled(!sfxEnabled);
                  playBeep(500);
                  addTelemetryLog(`Sound synthesizers toggled ${!sfxEnabled ? 'ON' : 'OFF'}.`);
                }}
              >
                🎵 SFX {sfxEnabled ? 'ON' : 'OFF'}
              </button>
              <button className="hud-btn" onClick={handleOpenSettings}>
                <SettingsIcon size={13} /> SETTINGS
              </button>
            </div>
          </header>

          {/* Main Grid View */}
          <div className="grid-container">
            {/* Left diagnostics panel */}
            <div className="panel">
              <div className="panel-header">
                <span>SUIT DIAGNOSTICS</span>
                <span style={{ fontSize: '0.65rem', color: 'var(--theme-bright)' }}>STARK-MK85</span>
              </div>
              <div className="panel-content">
                <div className="suit-box">
                  <svg className="suit-svg" viewBox="0 0 100 200" xmlns="http://www.w3.org/2000/svg">
                    <path className="suit-part" id="part-helmet" onClick={() => handleSuitPartClick('part-helmet')} d="M38,20 Q38,10 50,10 Q62,10 62,20 Q62,35 50,35 Q38,35 38,20 Z M44,22 L47,22 M53,22 L56,22" />
                    <path className="suit-part" id="part-neck" onClick={() => handleSuitPartClick('part-neck')} d="M44,35 L44,42 L56,42 L56,35 Z" />
                    <path className="suit-part" id="part-chest" onClick={() => handleSuitPartClick('part-chest')} d="M30,42 L70,42 L74,75 L64,105 L36,105 L26,75 Z" />
                    <polygon points="46,55 54,55 50,62" style={{ fill: 'var(--theme-bright)', filter: 'drop-shadow(0 0 3px var(--theme-glow))' }} />
                    <path className="suit-part" id="part-left-arm" onClick={() => handleSuitPartClick('part-left-arm')} d="M26,44 L16,70 L10,100 Q8,105 10,110 L14,110 L22,80 L28,52" />
                    <path className="suit-part" id="part-right-arm" onClick={() => handleSuitPartClick('part-right-arm')} d="M74,44 L84,70 L90,100 Q92,105 90,110 L86,110 L78,80 L72,52" />
                    <path className="suit-part" id="part-left-leg" onClick={() => handleSuitPartClick('part-left-leg')} d="M36,105 L32,145 L28,185 Q26,192 34,192 L44,192 L44,145 L46,105 Z" />
                    <path className="suit-part" id="part-right-leg" onClick={() => handleSuitPartClick('part-right-leg')} d="M64,105 L68,145 L72,185 Q74,192 66,192 L56,192 L56,145 L54,105 Z" />
                  </svg>
                </div>
                <div className="diagnostic-report">
                  <div className="diag-title">{diagPart.title}</div>
                  <div className="system-status-list">
                    {diagPart.details.map((item, idx) => (
                      <div className="system-status-item" key={idx}>
                        <span>{item.name}</span>
                        <span className={item.status === 'ok' ? 'status-ok' : item.status === 'warning' ? 'status-warning' : 'status-error'}>
                          {item.val}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </div>

            {/* Center HUD: Reactor Core + Chat Console */}
            <div className="center-hud">
              <div className="core-visualization">
                <div className="panel">
                  <div className="panel-header">
                    <span>ARC CORE V2</span>
                    <span style={{ fontSize: '0.65rem', color: 'var(--theme-bright)' }}>STARK IND.</span>
                  </div>
                  <div className="panel-content arc-reactor-card">
                    <svg className="arc-reactor-svg" onClick={handleArcReactorClick} viewBox="0 0 100 100">
                      <circle cx="50" cy="50" r="46" stroke="rgba(255,255,255,0.05)" strokeWidth="1.5" fill="none" />
                      <circle 
                        className="arc-outer-ring" 
                        cx="50" cy="50" r="44" 
                        style={{ animationDuration: systemPower === 200 ? '5s' : systemPower === 25 ? '60s' : '25s' }}
                      />
                      <circle 
                        className="arc-inner-ring" 
                        cx="50" cy="50" r="36" 
                        style={{ animationDuration: systemPower === 200 ? '3s' : systemPower === 25 ? '40s' : '15s' }}
                      />
                      <circle className="arc-glow-layer" cx="50" cy="50" r="32" strokeWidth="2" />
                      <circle 
                        className="arc-segments" 
                        cx="50" cy="50" r="26" 
                        style={{ animationDuration: systemPower === 200 ? '2s' : systemPower === 25 ? '30s' : '10s' }}
                      />
                      <polygon className="arc-center-tri" points="40,60 60,60 50,43" />
                      <circle className="arc-core" cx="50" cy="50" r="8" />
                    </svg>
                    <div className="reactor-meta">
                      <div className="reactor-title">POWER CAPACITY</div>
                      <div className="reactor-power-value">{systemPower.toFixed(1)}%</div>
                    </div>
                  </div>
                </div>

                <div className="panel">
                  <div className="panel-header">
                    <span>B.A.R.F. HOLOGRAM</span>
                    <span style={{ fontSize: '0.65rem', color: 'var(--theme-bright)' }}>3D STREAM</span>
                  </div>
                  <div className="hologram-card panel-content">
                    <canvas ref={canvasRef} id="hologram-canvas"></canvas>
                    <div className="canvas-overlay-text" id="hologram-mode-txt">3D WIREFRAME HELMET</div>
                  </div>
                </div>
              </div>

              {/* Chat Terminal Console */}
              <div className="panel console-panel">
                <div className="panel-header">
                  <span>SYSTEM COMMAND TERMINAL</span>
                  <span style={{ fontSize: '0.65rem', color: 'var(--theme-bright)' }}>DIRECT LINK</span>
                </div>
                <div className="chat-container">
                  {chatLogs.map((log, idx) => (
                    <div className={`message ${log.isUser ? 'user' : 'jarvis'}`} key={idx}>
                      <span className="sender-tag">{log.sender}</span>
                      <span>{log.text}</span>
                    </div>
                  ))}
                  <div ref={chatEndRef} />
                </div>
                
                <div className="console-action-btns">
                  <span className="action-badge" onClick={() => handleUserTextSubmit('diagnostics scan')}>SYSTEM SCAN</span>
                  <span className="action-badge" onClick={() => handleUserTextSubmit('repulsor test')}>REPULSOR TEST</span>
                  <span className="action-badge" onClick={() => handleUserTextSubmit('red alert')}>RED ALERT</span>
                  <span className="action-badge" onClick={() => handleUserTextSubmit('house party protocol')}>HOUSE PARTY</span>
                  <span className="action-badge" onClick={() => handleUserTextSubmit('clean slate protocol')}>CLEAN SLATE</span>
                  <span className="action-badge" onClick={() => handleUserTextSubmit('change theme to Friday')}>F.R.I.D.A.Y.</span>
                  <span className="action-badge" onClick={() => handleUserTextSubmit('change theme to Jarvis')}>J.A.R.V.I.S.</span>
                </div>

                <div className="chat-input-bar">
                  <input 
                    type="text" 
                    className="terminal-input" 
                    placeholder="Type command (e.g. 'turn on flashlight', 'open Spotify', 'red alert')..."
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') {
                        handleUserTextSubmit(e.target.value);
                        e.target.value = '';
                      }
                    }}
                    autoComplete="off"
                  />
                </div>
              </div>
            </div>

            {/* Right Panel: Power Controls + System logs */}
            <div className="panel">
              <div className="panel-header">
                <span>POWER CONSOLE</span>
                <span style={{ fontSize: '0.65rem', color: 'var(--theme-bright)' }}>ALLOCATION</span>
              </div>
              <div className="panel-content">
                <div className="slider-group">
                  <div className="slider-item">
                    <div className="slider-header">
                      <span className="slider-name">Flight Thrusters</span>
                      <span className="slider-val">{sliders.thrusters}%</span>
                    </div>
                    <div className="range-wrap">
                      <input 
                        type="range" 
                        className="hud-range" 
                        min="0" max="100" 
                        value={sliders.thrusters} 
                        onChange={(e) => handleSliderChange('thrusters', Number(e.target.value))}
                      />
                    </div>
                  </div>
                  <div className="slider-item">
                    <div className="slider-header">
                      <span className="slider-name">Repulsors Output</span>
                      <span className="slider-val">{sliders.repulsors}%</span>
                    </div>
                    <div className="range-wrap">
                      <input 
                        type="range" 
                        className="hud-range" 
                        min="0" max="100" 
                        value={sliders.repulsors} 
                        onChange={(e) => handleSliderChange('repulsors', Number(e.target.value))}
                      />
                    </div>
                  </div>
                  <div className="slider-item">
                    <div className="slider-header">
                      <span className="slider-name">Defensive Shields</span>
                      <span className="slider-val">{sliders.shields}%</span>
                    </div>
                    <div className="range-wrap">
                      <input 
                        type="range" 
                        className="hud-range" 
                        min="0" max="100" 
                        value={sliders.shields} 
                        onChange={(e) => handleSliderChange('shields', Number(e.target.value))}
                      />
                    </div>
                  </div>
                  <div className="slider-item">
                    <div className="slider-header">
                      <span className="slider-name">Life Support</span>
                      <span className="slider-val">{sliders.lifesupport}%</span>
                    </div>
                    <div className="range-wrap">
                      <input 
                        type="range" 
                        className="hud-range" 
                        min="0" max="100" 
                        value={sliders.lifesupport} 
                        onChange={(e) => handleSliderChange('lifesupport', Number(e.target.value))}
                      />
                    </div>
                  </div>
                </div>

                {/* System logs box */}
                <div style={{ borderTop: '1px solid var(--theme-dim)', marginTop: '10px', paddingTop: '10px', flex: 1, display: 'flex', flexDirection: 'column' }}>
                  <div className="panel-header" style={{ background: 'transparent', border: 'none', padding: '0 0 10px 0' }}>
                    <span>SYSTEM TELEMETRY LOGS</span>
                  </div>
                  <div className="system-log-box">
                    {telemetryLogs.map((log, idx) => (
                      <div className={`log-entry ${log.type}`} key={idx}>
                        [{log.time}] {log.text}
                      </div>
                    ))}
                    <div ref={telemetryEndRef} />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
