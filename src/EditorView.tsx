import React, { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import { Filesystem, Directory, Encoding } from '@capacitor/filesystem';
import { App as CapApp } from '@capacitor/app';
import { registerPlugin } from '@capacitor/core';
import { Note, DIR } from './Types';
import { Icon } from './Icons';

const SoraEditor = registerPlugin<any>('SoraEditor');

interface EditorViewProps {
    curId: string;
    notes: Note[];
    lang: 'zh' | 'en';
    t: any;
    theme: 'dark' | 'light';
    fontSize: number;
    showLineNums: boolean;
    autoSave: boolean;
    wordWrap: boolean;
    editorBg: string;
    setEditorBg: (v: string) => void;
    setFontSize: (v: number) => void;
    setShowLineNums: (v: boolean) => void;
    setAutoSave: (v: boolean) => void;
    setWordWrap: (v: boolean) => void;
    onClose: (save?: boolean) => void;
    reloadNotes: () => void;
}

const EditorView: React.FC<EditorViewProps> = ({
    curId, notes, lang, t, theme, fontSize, showLineNums, autoSave, wordWrap, editorBg, setEditorBg, setFontSize, setShowLineNums, setAutoSave, setWordWrap, onClose, reloadNotes
}) => {
    // --- State: UI Elements ---
    const [tocOpen, setTocOpen] = useState(false);
    const [searchOpen, setSearchOpen] = useState(false);
    const [moreOpen, setMoreOpen] = useState(false);
    const [findText, setFindText] = useState('');
    const [replaceText, setReplaceText] = useState('');
    const [matchIndex, setMatchIndex] = useState(-1);
    const [matches, setMatches] = useState<number[]>([]);
    const [showProps, setShowProps] = useState(false);
    const [showRename, setShowRename] = useState(false);
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [renameValue, setRenameValue] = useState('');
    const [propInfo, setPropInfo] = useState({ lines: 0, cursorLine: 0, chapter: '' });
    const [tocMode, setTocMode] = useState<'chars' | 'lines'>('chars');
    const [curChapterIndex, setCurChapterIndex] = useState(0);
    const [lastEditorPos, setLastEditorPos] = useState(0);
    const [showEditorSettings, setShowEditorSettings] = useState(false);
    const [showSaveConfirm, setShowSaveConfirm] = useState(false);
    const [liveLineCount, setLiveLineCount] = useState(0);
    const [isReadOnly, setIsReadOnly] = useState(false);
    const [showReadOnlyUI, setShowReadOnlyUI] = useState(true);
    const uiTimeout = useRef<number | null>(null);

    // --- State: Scrollbars ---
    const [editorScroll, setEditorScroll] = useState({ top: 0, height: 40, active: false });
    const [tocScroll, setTocScroll] = useState({ top: 0, height: 40, active: false });

    // --- Refs ---
    const textareaRef = useRef<HTMLTextAreaElement>(null);
    const tocListRef = useRef<HTMLDivElement>(null);
    const lineNumsRef = useRef<HTMLDivElement>(null);
    const timeouts = useRef<Record<string, number>>({});
    const drag = useRef({ active: false, startY: 0, startScroll: 0 });
    const lastNativeState = useRef({ curId: '', top: 0, left: 0, fontSize: 0 });

    const curNote = useMemo(() => notes.find(n => n.id === curId), [notes, curId]);

    const syncNativeText = async () => {
        try {
            const { content } = await SoraEditor.getText();
            if (content !== undefined && textareaRef.current) {
                textareaRef.current.value = content;
            }
        } catch (e) { }
    };

    const getLineCol = (text: string, pos: number) => {
        const sub = text.substring(0, pos);
        const lines = sub.split('\n');
        const line = lines.length - 1;
        const col = lines[line].length;
        return { line, col };
    };

    useEffect(() => {
        const sub = SoraEditor.addListener('onEditorClick', () => {
            if (isReadOnly) {
                setShowReadOnlyUI(prev => !prev);
            }
        });
        return () => { sub.remove(); };
    }, [isReadOnly]);

    useEffect(() => {
        if (isReadOnly && showReadOnlyUI) {
            if (uiTimeout.current) window.clearTimeout(uiTimeout.current);
            uiTimeout.current = window.setTimeout(() => {
                setShowReadOnlyUI(false);
            }, 2000);
        }
        return () => { if (uiTimeout.current) window.clearTimeout(uiTimeout.current); };
    }, [isReadOnly, showReadOnlyUI]);

    useEffect(() => {
        const shouldHideNative = moreOpen || tocOpen || showEditorSettings || showSaveConfirm || showRename || showProps;
        if (shouldHideNative) {
            syncNativeText().then(() => {
                SoraEditor.close().catch(() => { });
                if (textareaRef.current) {
                    textareaRef.current.style.opacity = '1';
                    textareaRef.current.style.pointerEvents = 'auto';
                }
            });
        } else {
            setTimeout(() => {
                const header = document.getElementById('editor-header');
                let topOffset = (isReadOnly && !showReadOnlyUI) ? 0 : (header ? header.clientHeight : 60);
                if (searchOpen && (!isReadOnly || showReadOnlyUI)) {
                    const searchPanel = document.querySelector('.search-replace-panel');
                    if (searchPanel) topOffset += searchPanel.clientHeight;
                    else topOffset += 100;
                }
                const currentContent = textareaRef.current ? textareaRef.current.value : (curNote?.content || '');
                const cursor = textareaRef.current ? textareaRef.current.selectionStart : 0;

                let bgToPass = editorBg;
                if (bgToPass === 'default' || bgToPass === 'transparent') {
                    bgToPass = theme === 'dark' ? '#000000' : '#FFFFFF';
                }

                SoraEditor.start({
                    content: currentContent,
                    top: topOffset,
                    left: showLineNums ? 0 : 12,
                    fontSize: fontSize,
                    showLineNumbers: showLineNums,
                    backgroundColor: bgToPass,
                    wordWrap: wordWrap,
                    editable: !isReadOnly
                }).then(() => {
                    const { line, col } = getLineCol(currentContent, cursor);
                    SoraEditor.setSelection({ line, column: col });
                    lastNativeState.current = { curId: curId || '', top: topOffset, left: 0, fontSize };
                }).catch((e: any) => console.error("Sora start failed", e));

                if (textareaRef.current) {
                    textareaRef.current.style.opacity = '0';
                    textareaRef.current.style.pointerEvents = 'none';
                }
            }, 100);
        }
    }, [moreOpen, tocOpen, searchOpen, showEditorSettings, showSaveConfirm, showRename, showProps, curId, curNote, fontSize, showLineNums, editorBg, theme, wordWrap, isReadOnly, showReadOnlyUI]);

    useEffect(() => {
        if (curNote?.content) {
            setLiveLineCount(curNote.content.split('\n').length);
        }
    }, [curNote]);

    const handleInput = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
        setLiveLineCount(e.target.value.split('\n').length);
        if (!autoSave) return;
        window.clearTimeout(timeouts.current.save);
        timeouts.current.save = window.setTimeout(async () => {
            try {
                await Filesystem.writeFile({ path: `${DIR}/${curId}`, data: e.target.value, directory: Directory.Documents, encoding: Encoding.UTF8 });
            } catch (e) { }
        }, 300);
    };

    const handleFind = useCallback((text: string, scroll = true, prefPos?: number) => {
        if (!text || !textareaRef.current) { setMatches([]); setMatchIndex(-1); return; }
        const val = textareaRef.current.value, newM: number[] = [];
        let p = val.indexOf(text);
        while (p !== -1) { newM.push(p); p = val.indexOf(text, p + 1); }
        setMatches(newM);
        if (newM.length) {
            const idx = prefPos !== undefined ? Math.max(0, newM.findIndex(m => m >= prefPos)) : 0;
            setMatchIndex(idx);
            if (scroll) {
                const pos = newM[idx];
                textareaRef.current.focus(); textareaRef.current.setSelectionRange(pos, pos + text.length);
                scrollToPos(pos);
            }
        } else setMatchIndex(-1);
    }, []);

    const moveMatch = (delta: number) => {
        if (!matches.length) return;
        const next = (matchIndex + delta + matches.length) % matches.length;
        setMatchIndex(next);
        const pos = matches[next];
        textareaRef.current?.focus(); textareaRef.current?.setSelectionRange(pos, pos + findText.length);
        scrollToPos(pos);
    };

    const handleReplace = (all = false) => {
        if (!textareaRef.current || (!all && matchIndex === -1)) return;
        const ta = textareaRef.current, content = ta.value;
        const nextVal = all ? content.split(findText).join(replaceText) : content.substring(0, matches[matchIndex]) + replaceText + content.substring(matches[matchIndex] + findText.length);
        ta.value = nextVal; handleInput({ target: { value: nextVal } } as any);
        setTimeout(() => handleFind(findText, !all), 0);
    };

    const scrollToPos = (pos: number) => {
        const ta = textareaRef.current; if (!ta) return;
        const ratio = pos / (ta.value.length || 1);
        ta.scrollTop = ratio * ta.scrollHeight;
        if (lineNumsRef.current) lineNumsRef.current.scrollTop = ta.scrollTop;
        const { line, col } = getLineCol(ta.value, pos);
        SoraEditor.setSelection({ line, column: col });
    };

    const syncScroll = (type: 'editor' | 'toc') => {
        const el = { editor: textareaRef, toc: tocListRef }[type].current;
        const state = { editor: editorScroll, toc: tocScroll }[type];
        const setter = { editor: setEditorScroll, toc: setTocScroll }[type];
        if (!el || drag.current.active) return;
        if (type === 'editor' && lineNumsRef.current) lineNumsRef.current.scrollTop = el.scrollTop;
        const ratio = el.scrollTop / (el.scrollHeight - el.clientHeight || 1);
        setter({ ...state, top: ratio * (el.clientHeight - state.height), active: true });
        window.clearTimeout(timeouts.current[type]);
        timeouts.current[type] = window.setTimeout(() => setter(s => ({ ...s, active: false })), 3000);
    };

    const handleDrag = (e: React.MouseEvent | React.TouchEvent, type: 'editor' | 'toc') => {
        const el = { editor: textareaRef, toc: tocListRef }[type].current;
        const setter = { editor: setEditorScroll, toc: setTocScroll }[type];
        const state = { editor: editorScroll, toc: tocScroll }[type];
        if (!el) return;
        drag.current = { active: true, startY: 'touches' in e ? e.touches[0].clientY : e.clientY, startScroll: el.scrollTop };
        setter(s => ({ ...s, active: true }));
        const onMove = (me: MouseEvent | TouchEvent) => {
            if (!drag.current.active) return;
            me.preventDefault();
            const cy = 'touches' in me ? me.touches[0].clientY : (me as MouseEvent).clientY;
            const dy = cy - drag.current.startY, range = el.scrollHeight - el.clientHeight, space = el.clientHeight - state.height;
            if (range > 0) el.scrollTop = drag.current.startScroll + (dy / space) * range;
            setter(s => ({ ...s, top: (el.scrollTop / range) * space }));
        };
        const onUp = () => {
            drag.current.active = false; setter(s => ({ ...s, active: false }));
            document.removeEventListener('mousemove', onMove); document.removeEventListener('mouseup', onUp);
            document.removeEventListener('touchmove', onMove); document.removeEventListener('touchend', onUp);
        };
        document.addEventListener('mousemove', onMove); document.addEventListener('mouseup', onUp);
        document.addEventListener('touchmove', onMove, { passive: false }); document.addEventListener('touchend', onUp);
    };

    const chapters = useMemo(() => {
        if (!curNote?.content) return [];
        if (tocMode === 'chars') {
            const len = curNote.content.length;
            const count = Math.ceil(len / 2000);
            return Array.from({ length: count }, (_, i) => ({
                index: i, pos: i * 2000, title: t.chapter.replace('{0}', (i + 1).toString())
            }));
        } else {
            const lines = curNote.content.split('\n');
            const count = Math.ceil(lines.length / 100);
            return Array.from({ length: count }, (_, i) => {
                const lineIndex = i * 100;
                const pos = lines.slice(0, lineIndex).reduce((acc, line) => acc + line.length + 1, 0);
                return { index: i, pos, title: lang === 'zh' ? `第 ${i * 100 + 1} 行` : `Line ${i * 100 + 1}` };
            });
        }
    }, [curNote?.content, t.chapter, tocMode, lang]);

    const handleShowProps = () => {
        if (!textareaRef.current) return;
        const txt = textareaRef.current.value, sel = textareaRef.current.selectionStart;
        setPropInfo({ lines: txt.split('\n').length, cursorLine: txt.substring(0, sel).split('\n').length, chapter: t.chapter.replace('{0}', (Math.floor(sel / 2000) + 1).toString()) });
        setShowProps(true); setMoreOpen(false);
    };

    const confirmRename = async () => {
        if (!curId || !renameValue || renameValue === curId) { setShowRename(false); return; }
        const final = renameValue.trim().endsWith('.txt') ? renameValue.trim() : renameValue.trim() + '.txt';
        try {
            const { data } = await Filesystem.readFile({ path: `${DIR}/${curId}`, directory: Directory.Documents, encoding: Encoding.UTF8 });
            await Filesystem.writeFile({ path: `${DIR}/${final}`, data: data as string, directory: Directory.Documents, encoding: Encoding.UTF8 });
            await Filesystem.deleteFile({ path: `${DIR}/${curId}`, directory: Directory.Documents });
            reloadNotes(); onClose(false);
        } catch (e) { alert(e); }
        setShowRename(false);
    };

    const confirmDelete = async () => {
        if (!curId) return;
        try {
            if (curNote?.inTrash) await Filesystem.deleteFile({ path: `${DIR}/${curId}`, directory: Directory.Documents });
            else {
                const name = curId.split('/').pop();
                const newPath = `.trash/${name}`;
                await Filesystem.rename({ from: `${DIR}/${curId}`, to: `${DIR}/${newPath}`, directory: Directory.Documents });
            }
            onClose(false); reloadNotes();
        } catch (e) { alert(e); }
        setShowDeleteConfirm(false);
    };

    useEffect(() => {
        const sub = CapApp.addListener('backButton', async () => {
            // In EditorView, handle back
            if (autoSave) {
                await syncNativeText();
                handleCloseEditor(true);
            } else {
                await syncNativeText();
                const ta = textareaRef.current;
                const content = ta?.value || '';
                const original = curNote?.content || '';
                if (content !== original) { setShowSaveConfirm(true); }
                else handleCloseEditor(false);
            }
        });
        return () => {
            sub?.then((h: any) => h.remove());
            // Crucial: Close native editor when unmounting!
            SoraEditor.close().catch(() => { });
            // Restore textarea visibility just in case
            if (textareaRef.current) {
                textareaRef.current.style.opacity = '1';
                textareaRef.current.style.pointerEvents = 'auto';
            }
        };
    }, [autoSave, curNote]);

    const handleCloseEditor = async (save = true) => {
        if (curId && textareaRef.current && save) {
            const content = textareaRef.current.value;
            const note = notes.find(n => n.id === curId);
            let finalId = curId;
            if (note?.isNew && content.trim()) {
                const now = new Date(), date = `${now.getFullYear()}.${(now.getMonth() + 1).toString().padStart(2, '0')}.${now.getDate().toString().padStart(2, '0')}`;
                const title = content.split('\n')[0].trim().substring(0, 15).replace(/[\\\/:*?"<>|]/g, '');
                const folder = curId.includes('/') ? curId.split('/')[0] + '/' : '';
                if (title) finalId = `${folder}${title} ${date}.txt`;
            }
            try {
                await Filesystem.writeFile({ path: `${DIR}/${finalId}`, data: content, directory: Directory.Documents, encoding: Encoding.UTF8 });
                if (finalId !== curId && curId.includes('temp_')) {
                    await Filesystem.deleteFile({ path: `${DIR}/${curId}`, directory: Directory.Documents }).catch(() => { });
                }
            } catch (e) { }
        }
        onClose();
    };


    const lineNumbers = useMemo(() => {
        let s = '';
        for (let i = 1; i <= liveLineCount; i++) s += i + '\n';
        return s;
    }, [liveLineCount]);

    return (
        <div className={`view ${curId ? '' : 'view-hidden'}`} onClick={() => { if (isReadOnly) setShowReadOnlyUI(prev => !prev); }}>
            <header id="editor-header" style={{ transform: (isReadOnly && !showReadOnlyUI) ? 'translateY(-100%)' : 'none', transition: 'transform 0.3s' }}>
                <button className="btn-icon" onClick={() => { if (!textareaRef.current) return; setCurChapterIndex(Math.floor((textareaRef.current.value.substring(0, textareaRef.current.selectionStart).split('\n').length - 1) / 100)); setTocOpen(true); }}><Icon d="M4 6h16M4 12h16M4 18h16" /></button>
                <div style={{ display: 'flex' }}>
                    <button className="btn-icon" onClick={() => { setSearchOpen(!searchOpen); if (!searchOpen && findText) setTimeout(() => { const p = textareaRef.current?.selectionStart || 0; setLastEditorPos(p); handleFind(findText, true, p); }, 0); }} style={{ marginLeft: 10 }}><Icon d="M21 21l-4.35-4.35M19 11a8 8 0 1 1-16 0 8 8 0 0 1 16 0z" /></button>
                    <button className="btn-icon" onClick={() => setMoreOpen(!moreOpen)} style={{ marginLeft: 10 }}><Icon d="M12 12m-1 0a1 1 0 1 0 2 0 1 1 0 1 0-2 0 M12 5m-1 0a1 1 0 1 0 2 0 1 1 0 1 0-2 0 M12 19m-1 0a1 1 0 1 0 2 0 1 1 0 1 0-2 0" /></button>
                </div>
            </header>
            {searchOpen && (
                <div className="search-replace-panel">
                    <div className="search-row">
                        <div className="search-input-wrapper">
                            <div onClick={() => { const p = textareaRef.current?.selectionStart || 0; setLastEditorPos(p); handleFind(findText, true, p); }} style={{ cursor: 'pointer', display: 'flex' }}><Icon d="M21 21l-4.35-4.35M19 11a8 8 0 1 1-16 0 8 8 0 0 1 16 0z" size={16} style={{ margin: '0 8px' }} /></div>
                            <input placeholder={t.find} value={findText} onChange={e => { setFindText(e.target.value); handleFind(e.target.value, false, lastEditorPos); }} onKeyDown={e => e.key === 'Enter' && handleFind(findText, true, lastEditorPos)} autoFocus />
                            <div className="search-meta">{matches.length ? `${matchIndex + 1}/${matches.length}` : '0/0'}</div>
                        </div>
                        <button className="btn-small" onClick={() => moveMatch(-1)}>↑</button><button className="btn-small" onClick={() => moveMatch(1)}>↓</button>
                    </div>
                    <div className="search-row"><div className="search-input-wrapper"><input placeholder={t.replace} style={{ paddingLeft: 12 }} value={replaceText} onChange={e => setReplaceText(e.target.value)} /></div><button className="btn-small" onClick={() => handleReplace()}>{t.replace}</button><button className="btn-small" onClick={() => handleReplace(true)}>{t.replaceAll}</button></div>
                </div>
            )}
            <div className="editor-container" style={{ position: 'relative', flex: 1, display: 'flex', overflow: 'hidden' }}>
                <textarea key={curId} ref={textareaRef} id="editor-area" placeholder={t.placeholder} defaultValue={curNote?.content || ''}
                    style={{ fontSize: `${fontSize}px`, paddingLeft: showLineNums ? 60 : 32, whiteSpace: wordWrap ? 'pre-wrap' : 'pre' }}
                    onChange={handleInput} onScroll={() => syncScroll('editor')} />
                {showLineNums && (
                    <div id="line-nums" ref={lineNumsRef} style={{ position: 'absolute', left: 0, top: 0, bottom: 0, width: 50, background: 'rgba(128,128,128,0.05)', color: 'var(--text-dim)', fontSize: fontSize * 0.7, padding: '20px 5px', textAlign: 'right', pointerEvents: 'none', lineHeight: 1.6, overflow: 'hidden', borderRight: '1px solid var(--border)', whiteSpace: 'pre' }}>
                        {lineNumbers}
                    </div>
                )}
                <div className={`custom-scrollbar ${editorScroll.active ? 'visible' : ''}`} style={{ top: 0, right: 2 }}><div className="scrollbar-thumb" style={{ top: editorScroll.top, height: editorScroll.height }} onMouseDown={e => handleDrag(e, 'editor')} onTouchStart={e => handleDrag(e, 'editor')} /></div>
            </div>

            {tocOpen && (
                <div className="toc-overlay" onClick={() => setTocOpen(false)} style={{ position: 'fixed', inset: 0, zIndex: 1000, background: 'rgba(0,0,0,0.5)' }}>
                    <div className="toc-sidebar" onClick={e => e.stopPropagation()}>
                        <div className="toc-header" style={{ padding: 'calc(15px + env(safe-area-inset-top)) 12px 15px', borderBottom: '1px solid var(--border)', display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <h3 style={{ margin: 0, fontSize: '1.1rem', flexShrink: 0 }}>{t.toc}</h3>
                            <div className="toc-mode-switch" style={{ display: 'flex', background: 'var(--bg)', borderRadius: '8px', padding: '2px', border: '1px solid var(--border)', flex: 1, minWidth: 0 }}>
                                <button onClick={() => setTocMode('chars')} style={{ flex: 1, padding: '4px 2px', border: 'none', borderRadius: '6px', fontSize: '0.75rem', background: tocMode === 'chars' ? 'var(--surface)' : 'transparent', color: tocMode === 'chars' ? 'var(--primary)' : 'var(--text-dim)', fontWeight: tocMode === 'chars' ? 'bold' : 'normal', whiteSpace: 'nowrap' }}>{lang === 'zh' ? '按字' : 'Chars'}</button>
                                <button onClick={() => setTocMode('lines')} style={{ flex: 1, padding: '4px 2px', border: 'none', borderRadius: '6px', fontSize: '0.75rem', background: tocMode === 'lines' ? 'var(--surface)' : 'transparent', color: tocMode === 'lines' ? 'var(--primary)' : 'var(--text-dim)', fontWeight: tocMode === 'lines' ? 'bold' : 'normal', whiteSpace: 'nowrap' }}>{lang === 'zh' ? '按行' : 'Lines'}</button>
                            </div>
                            <button className="btn-icon" onClick={() => setTocOpen(false)} style={{ padding: 4, flexShrink: 0 }}>✕</button>
                        </div>
                        <div className="toc-list" ref={tocListRef} onScroll={() => syncScroll('toc')} style={{ flex: 1, overflowY: 'auto' }}>
                            {chapters.map(ch => (
                                <div key={ch.index} className={`toc-item ${ch.index === curChapterIndex ? 'active' : ''}`} onClick={() => { if (textareaRef.current) { textareaRef.current.setSelectionRange(ch.pos, ch.pos); scrollToPos(ch.pos); } setTocOpen(false); }}>{ch.title}</div>
                            ))}
                        </div>
                        <div className={`custom-scrollbar ${tocScroll.active ? 'visible' : ''}`} style={{ top: 60 }}><div className="scrollbar-thumb" style={{ top: tocScroll.top, height: tocScroll.height }} onMouseDown={e => handleDrag(e, 'toc')} onTouchStart={e => handleDrag(e, 'toc')} /></div>
                    </div>
                </div>
            )}

            {moreOpen && (
                <div className="more-menu-overlay" onClick={() => setMoreOpen(false)}><div className="more-menu" onClick={e => e.stopPropagation()}>
                    <div className="menu-item" onClick={() => handleCloseEditor()}><Icon d="M19 12H5M12 19l-7-7 7-7" style={{ marginRight: 12 }} size={20} />{t.back}</div>
                    <div className="menu-item" onClick={() => { SoraEditor.undo(); setMoreOpen(false); }}><Icon d="M9 13l-4-4 4-4M5 9h11a4 4 0 010 8h-1" style={{ marginRight: 12 }} size={20} />{t.undo}</div>
                    <div className="menu-item" onClick={() => { SoraEditor.redo(); setMoreOpen(false); }}><Icon d="M15 13l4-4-4-4M19 9H8a4 4 0 000 8h1" style={{ marginRight: 12 }} size={20} />{t.redo}</div>
                    {!curNote?.inTrash && <div className="menu-item" onClick={() => { setRenameValue(curNote?.id || ''); setShowRename(true); setMoreOpen(false); }}><Icon d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7 M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" style={{ marginRight: 12 }} size={20} />{t.rename}</div>}
                    <div className="menu-item" onClick={() => { setShowDeleteConfirm(true); setMoreOpen(false); }} style={{ color: '#ff4d4f' }}><Icon d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" style={{ marginRight: 12 }} size={20} />{curNote?.inTrash ? t.permDelete : t.moveToTrash}</div>
                    <div className="menu-item" onClick={handleShowProps}><Icon d="M12 12m-9 0a9 9 0 1 0 18 0 9 9 0 1 0-18 0 M12 16v-4 M12 8h.01" style={{ marginRight: 12 }} size={20} />{t.properties}</div>
                    <div className="menu-item" onClick={() => { setIsReadOnly(!isReadOnly); setShowReadOnlyUI(true); setMoreOpen(false); }}><Icon d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z" style={{ marginRight: 12 }} size={20} />{t.readOnly}: {isReadOnly ? 'ON' : 'OFF'}</div>
                    <div className="menu-item" onClick={() => { setShowEditorSettings(true); setMoreOpen(false); }}><Icon d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z M12 12m-3 0a3 3 0 1 0 6 0 3 3 0 1 0-6 0" style={{ marginRight: 12 }} size={20} />{t.editorSettings}</div>
                </div></div>
            )}
            {showEditorSettings && (
                <div className="modal-overlay" onClick={() => setShowEditorSettings(false)}><div className="modal-content" onClick={e => e.stopPropagation()}>
                    <h4>{t.editorSettings}</h4>
                    <div style={{ marginBottom: 20 }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 10 }}><span>{t.fontSize}: {fontSize}px</span><button className="btn-small" onClick={() => setFontSize(18)}>{t.reset}</button></div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                            <button className="btn-icon" onClick={() => setFontSize(Math.max(12, fontSize - 1))} style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '8px' }}><Icon d="M5 12h14" size={18} /></button>
                            <input type="range" min="12" max="36" value={fontSize} onInput={e => setFontSize(Number((e.target as HTMLInputElement).value))} style={{ flex: 1, accentColor: 'var(--primary)' }} />
                            <button className="btn-icon" onClick={() => setFontSize(Math.min(36, fontSize + 1))} style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '8px' }}><Icon d="M12 5v14M5 12h14" size={18} /></button>
                        </div>
                    </div>
                    <div>
                        <div style={{ marginBottom: 10 }}>{t.bgColor}</div>
                        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
                            {[
                                { name: t.white, color: '#FFFFFF' },
                                { name: t.yellow, color: '#F8F1E7' },
                                { name: t.green, color: '#E1EAD2' },
                                { name: t.blue, color: '#D1D7DA' },
                                { name: t.black, color: '#000000' }
                            ].map(c => (
                                <div key={c.color} onClick={() => setEditorBg(c.color)} style={{ width: 44, height: 44, borderRadius: '50%', background: c.color, border: editorBg === c.color ? '3px solid var(--primary)' : '1px solid var(--border)', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyItems: 'center', transition: 'transform 0.2s' }} className={editorBg === c.color ? 'scale-up' : ''}>
                                    {editorBg === c.color && <Icon d="M5 13l4 4L19 7" color={c.color === '#000000' ? 'white' : 'black'} style={{ margin: 'auto' }} size={24} />}
                                </div>
                            ))}
                        </div>
                    </div>
                    <div style={{ display: 'flex', gap: 15, marginTop: 20, flexWrap: 'wrap' }}>
                        <button className={`theme-btn ${autoSave ? '' : 'btn-dim'}`} onClick={() => setAutoSave(!autoSave)} style={{ flex: '1 1 40%', fontSize: '0.8rem' }}>{t.autoSave}: {autoSave ? 'ON' : 'OFF'}</button>
                        <button className={`theme-btn ${wordWrap ? '' : 'btn-dim'}`} onClick={() => setWordWrap(!wordWrap)} style={{ flex: '1 1 40%', fontSize: '0.8rem' }}>{t.wordWrap}: {wordWrap ? 'ON' : 'OFF'}</button>
                        <button className={`theme-btn ${showLineNums ? '' : 'btn-dim'}`} onClick={() => setShowLineNums(!showLineNums)} style={{ flex: '1 1 40%', fontSize: '0.8rem' }}>{t.lineNum}: {showLineNums ? 'ON' : 'OFF'}</button>
                    </div>
                    <button className="modal-close" onClick={() => setShowEditorSettings(false)}>{t.close}</button>
                </div></div>
            )}
            {showSaveConfirm && (
                <div className="modal-overlay" onClick={() => setShowSaveConfirm(false)}><div className="modal-content" onClick={e => e.stopPropagation()}>
                    <h4>{t.saveConfirm}</h4>
                    <div style={{ display: 'flex', gap: 10 }}>
                        <button className="modal-close" style={{ background: 'var(--surface)', color: 'var(--text)', flex: 1, marginTop: 0 }} onClick={() => handleCloseEditor(false)}>{t.discard}</button>
                        <button className="modal-close" style={{ flex: 1, marginTop: 0 }} onClick={() => handleCloseEditor(true)}>{t.save}</button>
                    </div>
                </div></div>
            )}

            {showProps && curNote && <div className="modal-overlay" onClick={() => setShowProps(false)}><div className="modal-content" onClick={e => e.stopPropagation()}><h4>{t.fileInfo}</h4><div className="prop-row"><span>{t.title}:</span> {curNote.title}</div><div className="prop-row"><span>{t.chars}:</span> {curNote.content.length}</div><div className="prop-row"><span>{t.lines}:</span> {propInfo.lines}</div><div className="prop-row"><span>{t.cursorLine}:</span> {propInfo.cursorLine}</div><div className="prop-row"><span>{t.toc}:</span> {propInfo.chapter}</div><div className="prop-row"><span>{t.modified}:</span> {new Date(curNote.time).toLocaleString()}</div><button className="modal-close" onClick={() => setShowProps(false)}>{t.close}</button></div></div>}
            {showRename && <div className="modal-overlay" onClick={() => setShowRename(false)}><div className="modal-content" onClick={e => e.stopPropagation()}><h4>{t.rename}</h4><input className="search-input" value={renameValue} onChange={e => setRenameValue(e.target.value)} autoFocus style={{ marginBottom: 20 }} /><div style={{ display: 'flex', gap: 10 }}><button className="modal-close" style={{ background: 'var(--surface)', color: 'var(--text)', flex: 1, marginTop: 0 }} onClick={() => setShowRename(false)}>{t.cancel}</button><button className="modal-close" style={{ flex: 1, marginTop: 0 }} onClick={confirmRename}>{t.ok}</button></div></div></div>}
            {showDeleteConfirm && <div className="modal-overlay" onClick={() => setShowDeleteConfirm(false)}><div className="modal-content" onClick={e => e.stopPropagation()}><h4>{curNote?.inTrash ? t.trashConfirm : t.delConfirm}</h4><div style={{ display: 'flex', gap: 10, marginTop: 10 }}><button className="modal-close" style={{ background: 'var(--surface)', color: 'var(--text)', flex: 1, marginTop: 0 }} onClick={() => setShowDeleteConfirm(false)}>{t.cancel}</button><button className="modal-close" style={{ background: '#ff4d4f', flex: 1, marginTop: 0 }} onClick={confirmDelete}>{t.ok}</button></div></div></div>}
        </div>
    );
};

export default EditorView;
