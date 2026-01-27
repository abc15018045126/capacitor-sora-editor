import React, { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import { Filesystem, Directory, Encoding } from '@capacitor/filesystem';
import { App as CapApp } from '@capacitor/app';
import { registerPlugin } from '@capacitor/core';
import { Note, DIR } from './Types';
import { Icon } from './Icons';
import ListView from './ListView';
import EditorView from './EditorView';

import ComposeEditor from './ComposeEditor';

const OpenFolder = registerPlugin<any>('OpenFolder');

const App: React.FC = () => {
    // --- State: App Flow & Data ---
    const [view, setView] = useState<'list' | 'editor' | 'settings'>('list');
    const [notes, setNotes] = useState<Note[]>([]);
    const [groups, setGroups] = useState<string[]>([]);
    const [curGroup, setCurGroup] = useState<string | null>(null);
    const [curId, setCurId] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [docPath, setDocPath] = useState('...');
    const [lang, setLang] = useState<'zh' | 'en'>(() => (localStorage.getItem('lang') as any) || 'zh');
    const [theme, setTheme] = useState<'dark' | 'light'>(() => (localStorage.getItem('theme') as any) || 'dark');

    // --- State: UI Elements (List level) ---
    const [sidebarOpen, setSidebarOpen] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');
    const [showNewGroup, setShowNewGroup] = useState(false);
    const [newGroupValue, setNewGroupValue] = useState('');
    const [targetGroup, setTargetGroup] = useState<string | null>(null);
    const [showGroupMenu, setShowGroupMenu] = useState(false);
    const [showGroupRename, setShowGroupRename] = useState(false);
    const [groupRenameValue, setGroupRenameValue] = useState('');
    const [showGroupDeleteConfirm, setShowGroupDeleteConfirm] = useState(false);
    const [isSelectMode, setIsSelectMode] = useState(false);
    const [selectedIds, setSelectedIds] = useState<string[]>([]);
    const [showMoveToModal, setShowMoveToModal] = useState(false);
    const [fontSize, setFontSize] = useState<number>(() => Number(localStorage.getItem('fontSize')) || 18);
    const [editorBg, setEditorBg] = useState<string>(() => localStorage.getItem('editorBg') || 'default');
    const [showEditorSettings, setShowEditorSettings] = useState(false);
    const [autoSave, setAutoSave] = useState<boolean>(() => localStorage.getItem('autoSave') !== 'false');
    const [showLineNums, setShowLineNums] = useState<boolean>(() => localStorage.getItem('showLineNums') === 'true');
    const [wordWrap, setWordWrap] = useState<boolean>(() => localStorage.getItem('wordWrap') === 'true');

    // --- State: Scrollbars (List/Sidebar) ---
    const [listScroll, setListScroll] = useState({ top: 0, height: 40, active: false });
    const [tocScroll, setTocScroll] = useState({ top: 0, height: 40, active: false });

    // --- Refs ---
    const listRef = useRef<HTMLDivElement>(null);
    const tocListRef = useRef<HTMLDivElement>(null);
    const timeouts = useRef<Record<string, number>>({});
    const drag = useRef({ active: false, startY: 0, startScroll: 0 });

    const t = ({
        zh: {
            title: '便签', search: '搜索内容...', noNotes: '还没有便签', noMatch: '没找到匹配项', settings: '设置',
            theme: '颜色主题', lang: '语言 / Lang', path: '数据目录 (点击打开)：', delConfirm: '确认删除？',
            placeholder: '写点什么...', dark: '深色', light: '浅色', back: '返回', newNote: '新便签.txt',
            repo: '开源地址 (GitHub)', deleteNote: '删除', toc: '目录', find: '查找', replace: '替换',
            replaceAll: '全部替换', more: '更多', rename: '重命名', properties: '属性', renamePrompt: '请输入新文件名',
            fileInfo: '文件信息', close: '关闭', chapter: '第 {0} 章', chars: '字数', modified: '最后修改',
            lines: '总行数', cursorLine: '光标所在行', ok: '确定', cancel: '取消', allNotes: '全部便签',
            uncategorized: '未分类', newGroup: '新建分组', groups: '分组', enterGroupName: '输入分组名',
            trash: '回收站', moveToTrash: '移入回收站', permDelete: '彻底删除', restore: '恢复', trashConfirm: '彻底删除无法恢复，确认？',
            groupRename: '重命名分组', groupDelete: '删除分组', groupDelConfirm: '分组内文件将移入回收站，确认删除？',
            selectAll: '全选', deselectAll: '取消全选', moveTo: '移动到', batchDelete: '彻底删除', batchTrash: '移入回收站',
            selectItems: '已选择 {0} 项', moveNote: '移动便签', editorSettings: '编辑器设置', fontSize: '字体大小',
            reset: '重置', bgColor: '背景颜色', white: '白色', yellow: '米黄', green: '豆绿', blue: '清爽', black: '黑色',
            undo: '撤销', redo: '重做', autoSave: '自动保存', lineNum: '显示行号', saveConfirm: '是否保存当前修改？', save: '保存', discard: '放弃',
            wordWrap: '自动换行', readOnly: '只读模式'
        },
        en: {
            title: 'Notes', search: 'Search...', noNotes: 'No notes found', noMatch: 'No matches found', settings: 'Settings',
            theme: 'Appearance', lang: 'Language', path: 'Storage (Click to open):', delConfirm: 'Delete this note?',
            placeholder: 'Type here...', dark: 'Dark', light: 'Light', back: 'Back', newNote: 'New Note.txt',
            repo: 'Source Code (GitHub)', deleteNote: 'Delete', toc: 'Contents', find: 'Find', replace: 'Replace',
            replaceAll: 'Replace All', more: 'More', rename: 'Rename', properties: 'Properties', renamePrompt: 'Enter new filename',
            fileInfo: 'File Info', close: 'Close', chapter: 'Chapter {0}', chars: 'Characters', modified: 'Last Modified',
            lines: 'Total Lines', cursorLine: 'Cursor Line', ok: 'OK', cancel: 'Cancel', allNotes: 'All Notes',
            uncategorized: 'Uncategorized', newGroup: 'New Group', groups: 'Groups', enterGroupName: 'Group Name',
            trash: 'Recycle Bin', moveToTrash: 'Move to Trash', permDelete: 'Delete Permanently', restore: 'Restore', trashConfirm: 'Cannot undo. Delete?',
            groupRename: 'Rename Group', groupDelete: 'Delete Group', groupDelConfirm: 'Files will be moved to trash. Delete?',
            selectAll: 'All', deselectAll: 'None', moveTo: 'Move To', batchDelete: 'Delete', batchTrash: 'Trash',
            selectItems: '{0} Selected', moveNote: 'Move Notes', editorSettings: 'Editor Settings', fontSize: 'Font Size',
            reset: 'Reset', bgColor: 'Background Color', white: 'White', yellow: 'Yellow', green: 'Green', blue: 'Blue', black: 'Black',
            undo: 'Undo', redo: 'Redo', autoSave: 'Auto Save', lineNum: 'Line Numbers', saveConfirm: 'Save changes?', save: 'Save', discard: 'Discard',
            wordWrap: 'Word Wrap', readOnly: 'Read-only'
        }
    }[lang]);

    useEffect(() => {
        document.documentElement.setAttribute('data-theme', theme);
        localStorage.setItem('theme', theme);
        localStorage.setItem('lang', lang);
        localStorage.setItem('fontSize', fontSize.toString());
        localStorage.setItem('editorBg', editorBg);
        localStorage.setItem('autoSave', autoSave.toString());
        localStorage.setItem('showLineNums', showLineNums.toString());
        localStorage.setItem('wordWrap', wordWrap.toString());
    }, [theme, lang, fontSize, editorBg, autoSave, showLineNums, wordWrap]);

    const reloadNotes = useCallback(async () => {
        try {
            await Filesystem.mkdir({ path: DIR, directory: Directory.Documents, recursive: true }).catch(() => { });
            await Filesystem.mkdir({ path: `${DIR}/.trash`, directory: Directory.Documents, recursive: true }).catch(() => { });
            const { files } = await Filesystem.readdir({ path: DIR, directory: Directory.Documents });

            const dirs = files.filter(f => f.type === 'directory' && f.name !== '.trash').map(d => d.name);
            setGroups(dirs);

            const loadFile = async (name: string, folder = '', isTrash = false) => {
                const path = folder ? `${DIR}/${folder}/${name}` : `${DIR}/${name}`;
                try {
                    const { data } = await Filesystem.readFile({ path, directory: Directory.Documents, encoding: Encoding.UTF8 });
                    const { mtime } = await Filesystem.stat({ path, directory: Directory.Documents });

                    let time = mtime || Date.now();
                    // Capacitor on some platforms/versions might return seconds instead of ms
                    if (time > 0 && time < 10000000000) time *= 1000;

                    return { id: folder ? `${folder}/${name}` : name, title: name, content: data as string, time, inTrash: isTrash };
                } catch (e) {
                    console.warn("Failed to load note:", path, e);
                    return { id: folder ? `${folder}/${name}` : name, title: name, content: '', time: Date.now(), inTrash: isTrash };
                }
            };

            const rootNotes = await Promise.all(files.filter(f => f.name !== '.trash' && !f.name.startsWith('.') && f.type !== 'directory' && !dirs.includes(f.name)).map(f => loadFile(f.name)));
            const groupNotesArrays = await Promise.all(dirs.map(async d => {
                const { files: subFiles } = await Filesystem.readdir({ path: `${DIR}/${d}`, directory: Directory.Documents });
                return Promise.all(subFiles.filter(f => f.type !== 'directory' && !f.name.startsWith('.')).map(f => loadFile(f.name, d)));
            }));
            const { files: trashFiles } = await Filesystem.readdir({ path: `${DIR}/.trash`, directory: Directory.Documents }).catch(() => ({ files: [] }));
            const trashNotes = await Promise.all(trashFiles.filter(f => f.type !== 'directory' && !f.name.startsWith('.')).map(f => loadFile(f.name, '.trash', true)));

            setNotes([...rootNotes, ...groupNotesArrays.flat(), ...trashNotes].sort((a, b) => b.time - a.time));
            const { uri } = await Filesystem.getUri({ path: DIR, directory: Directory.Documents });
            setDocPath(uri);
        } catch (e) { console.error(e); } finally { setIsLoading(false); }
    }, []);

    useEffect(() => {
        const checkPerms = async () => {
            try {
                const status = await Filesystem.checkPermissions();
                if (status.publicStorage !== 'granted') await Filesystem.requestPermissions();
                await OpenFolder.requestAllFilesAccess();
            } catch (e) { console.error('Permission check failed', e); }
        };
        checkPerms(); reloadNotes();
    }, [reloadNotes]);

    useEffect(() => {
        const sub = CapApp.addListener('backButton', async () => {
            if (view === 'editor') { /* EditorView handles its own back logic via props if needed, or we handle here */ }
            else if (view === 'settings' || sidebarOpen) { setView('list'); setSidebarOpen(false); }
            else CapApp.exitApp();
        });
        return () => { sub.then(h => h.remove()); };
    }, [view, sidebarOpen]);

    // Reload notes when app resumes (e.g., returning from native editor)
    useEffect(() => {
        const sub = CapApp.addListener('resume', () => {
            reloadNotes();
        });
        return () => { sub.then(h => h.remove()); };
    }, [reloadNotes]);

    const createNewNote = async () => {
        const prefix = (curGroup && curGroup !== '') ? `${curGroup}/` : '';
        // Use a timestamp based name that matches the native "New File" detection regex
        const filename = `${Date.now()}.txt`;
        const id = `${prefix}${filename}`;

        try {
            await Filesystem.writeFile({
                path: `${DIR}/${id}`,
                data: '',
                directory: Directory.Documents,
                encoding: Encoding.UTF8
            });

            const { uri } = await Filesystem.getUri({
                path: `${DIR}/${id}`,
                directory: Directory.Documents
            });

            await ComposeEditor.openEditor({ filePath: uri, autoFocus: true });
        } catch (e) {
            console.error("Failed to create new note", e);
            alert("Failed to create note: " + e);
        }
    };

    const createGroup = async () => {
        if (!newGroupValue) return;
        try {
            await Filesystem.mkdir({ path: `${DIR}/${newGroupValue}`, directory: Directory.Documents, recursive: true });
            setShowNewGroup(false); setNewGroupValue(''); reloadNotes();
        } catch (e) { alert(e); }
    };

    const confirmRenameGroup = async () => {
        if (!targetGroup || !groupRenameValue || targetGroup === groupRenameValue) { setShowGroupRename(false); return; }
        try {
            await Filesystem.rename({ from: `${DIR}/${targetGroup}`, to: `${DIR}/${groupRenameValue}`, directory: Directory.Documents });
            if (curGroup === targetGroup) setCurGroup(groupRenameValue);
            reloadNotes();
        } catch (e) { alert(e); }
        setShowGroupRename(false); setTargetGroup(null);
    };

    const confirmDeleteGroup = async () => {
        if (!targetGroup) return;
        try {
            const { files } = await Filesystem.readdir({ path: `${DIR}/${targetGroup}`, directory: Directory.Documents });
            await Promise.all(files.filter(f => f.name.endsWith('.txt')).map(async f => {
                const oldPath = `${DIR}/${targetGroup}/${f.name}`;
                const newPath = `${DIR}/.trash/${f.name}`;
                await Filesystem.rename({ from: oldPath, to: newPath, directory: Directory.Documents }).catch(async () => {
                    await Filesystem.rename({ from: oldPath, to: `${DIR}/.trash/${Date.now()}_${f.name}`, directory: Directory.Documents });
                });
            }));
            await Filesystem.rmdir({ path: `${DIR}/${targetGroup}`, directory: Directory.Documents, recursive: true });
            if (curGroup === targetGroup) setCurGroup(null);
            reloadNotes();
        } catch (e) { alert(e); }
        setShowGroupDeleteConfirm(false); setTargetGroup(null);
    };

    const bulkProcess = async (action: 'trash' | 'delete' | 'move', target?: string) => {
        try {
            await Promise.all(selectedIds.map(async id => {
                const name = id.split('/').pop()!;
                if (action === 'trash') await Filesystem.rename({ from: `${DIR}/${id}`, to: `${DIR}/.trash/${name}`, directory: Directory.Documents }).catch(() => { });
                else if (action === 'delete') await Filesystem.deleteFile({ path: `${DIR}/${id}`, directory: Directory.Documents });
                else if (action === 'move') {
                    const newPath = target === '' ? name : `${target}/${name}`;
                    if (id !== newPath) await Filesystem.rename({ from: `${DIR}/${id}`, to: `${DIR}/${newPath}`, directory: Directory.Documents });
                }
            }));
            setIsSelectMode(false); setSelectedIds([]); reloadNotes();
        } catch (e) { alert(e); }
    };

    const syncScroll = (type: 'list' | 'toc') => {
        const el = { list: listRef, toc: tocListRef }[type].current;
        const state = { list: listScroll, toc: tocScroll }[type];
        const setter = { list: setListScroll, toc: setTocScroll }[type];
        if (!el || drag.current.active) return;
        const ratio = el.scrollTop / (el.scrollHeight - el.clientHeight || 1);
        setter({ ...state, top: ratio * (el.clientHeight - state.height), active: true });
        window.clearTimeout(timeouts.current[type]);
        timeouts.current[type] = window.setTimeout(() => setter(s => ({ ...s, active: false })), 3000);
    };

    const handleDrag = (e: React.MouseEvent | React.TouchEvent, type: 'list' | 'toc') => {
        const el = { list: listRef, toc: tocListRef }[type].current;
        const setter = { list: setListScroll, toc: setTocScroll }[type];
        const state = { list: listScroll, toc: tocScroll }[type];
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

    const filteredNotes = useMemo(() => {
        const q = searchQuery.toLowerCase().trim();
        let list = notes;
        if (curGroup === '__TRASH__') list = list.filter(n => n.inTrash);
        else {
            list = list.filter(n => !n.inTrash);
            if (curGroup !== null) list = list.filter(n => curGroup === '' ? !n.id.includes('/') : n.id.startsWith(curGroup + '/'));
        }
        return q ? list.filter(n => n.title.toLowerCase().includes(q) || n.content.toLowerCase().includes(q)) : list;
    }, [notes, searchQuery, curGroup]);

    if (isLoading) return null;

    return (
        <div className="app">
            {view === 'list' && (
                <ListView
                    t={t} lang={lang} theme={theme} curGroup={curGroup} searchQuery={searchQuery} setSearchQuery={setSearchQuery}
                    filteredNotes={filteredNotes} selectedIds={selectedIds} isSelectMode={isSelectMode}
                    setIsSelectMode={setIsSelectMode} setSelectedIds={setSelectedIds}
                    onOpenNote={async (id) => {
                        const note = notes.find(n => n.id === id);
                        if (note) {
                            try {
                                // Get the actual file URI
                                const filePath = `${DIR}/${id}`;
                                const { uri } = await Filesystem.getUri({
                                    path: filePath,
                                    directory: Directory.Documents
                                });
                                await ComposeEditor.openEditor({ filePath: uri });
                            } catch (e) {
                                console.error('Failed to open editor:', e);
                            }
                        }
                    }}
                    onSidebarOpen={() => setSidebarOpen(true)}
                    onSettingsOpen={() => setView('settings')}
                    onCreateNote={createNewNote}
                    onBulkProcess={bulkProcess}
                    setShowMoveToModal={setShowMoveToModal}
                    listRef={listRef} listScroll={listScroll} handleDrag={handleDrag} syncScroll={syncScroll} timeouts={timeouts}
                    drag={drag}
                />
            )}

            {view === 'editor' && curId && (
                <EditorView
                    curId={curId} notes={notes} lang={lang} t={t} theme={theme} fontSize={fontSize}
                    showLineNums={showLineNums} autoSave={autoSave} editorBg={editorBg}
                    wordWrap={wordWrap} setWordWrap={setWordWrap}
                    setEditorBg={setEditorBg} setFontSize={setFontSize}
                    setShowLineNums={setShowLineNums} setAutoSave={setAutoSave}
                    onClose={() => { setView('list'); setCurId(null); reloadNotes(); }}
                    reloadNotes={reloadNotes}
                />
            )}

            {view === 'settings' && (
                <div className="view">
                    <header><button className="btn-icon" onClick={() => setView('list')}><Icon d="M19 12H5M12 19l-7-7 7-7" /></button><h1>{t.settings}</h1><div style={{ width: 44 }}></div></header>
                    <div className="settings-content">
                        <div className="settings-row"><span>{t.theme}</span><button className="theme-btn" onClick={() => setTheme(th => th === 'dark' ? 'light' : 'dark')}>{theme === 'dark' ? t.light : t.dark}</button></div>
                        <div className="settings-row"><span>{t.lang}</span><button className="theme-btn" onClick={() => setLang(l => l === 'zh' ? 'en' : 'zh')}>{lang === 'zh' ? 'English' : '中文'}</button></div>
                        <div className="settings-row" onClick={() => window.open('https://github.com/abc15018045126/notes', '_blank')} style={{ cursor: 'pointer' }}><span>{t.repo}</span><Icon d="M12 .3a12 12 0 0 0-3.8 23.4c.6.1.8-.3.8-.6v-2c-3.3.7-4-1.4-4-1.4-.6-1.4-1.4-1.8-1.4-1.8-1-.7.1-.7.1-.7 1.2.1 1.9 1.2 1.9 1.2 1 1.8 2.8 1.3 3.5 1 .1-.8.4-1.3.8-1.6-2.7-.3-5.5-1.3-5.5-6 0-1.3.5-2.4 1.2-3.2-.1-.3-.5-1.5.1-3.2 0 0 1-.3 3.3 1.2 1-.3 2-.4 3-.4s2 .1 3 .4c2.3-1.5 3.3-1.2 3.3-1.2.7 1.7.2 2.9.1 3.2.8.8 1.2 1.9 1.2 3.2 0 4.6-2.8 5.6-5.5 5.9.4.4.8 1.1.8 2.2V23c0 .3.2.7.8.6A12 12 0 0 0 12 .3" size={20} /></div>
                        <span className="path-label">{t.path}</span><div className="path-text" style={{ cursor: 'pointer', border: '1px solid var(--primary)', marginTop: 5 }} onClick={async () => { try { await OpenFolder.open(); } catch (e) { alert('Error: ' + e); } }}>{docPath}</div>
                    </div>
                </div>
            )}

            {sidebarOpen && (
                <div className="toc-overlay" onClick={() => setSidebarOpen(false)}>
                    <div className="toc-sidebar" onClick={e => e.stopPropagation()}>
                        <div className="toc-header" style={{ padding: 'calc(15px + env(safe-area-inset-top)) 20px 15px' }}>
                            <h3 style={{ margin: 0 }}>{t.groups}</h3>
                            <button className="btn-icon" onClick={() => setSidebarOpen(false)}>✕</button>
                        </div>
                        <div className="toc-list" ref={tocListRef} onScroll={() => syncScroll('toc')}>
                            <div className={`toc-item ${curGroup === null ? 'active' : ''}`} onClick={() => { setCurGroup(null); setSidebarOpen(false); }}>{t.allNotes}</div>
                            <div className={`toc-item ${curGroup === '' ? 'active' : ''}`} onClick={() => { setCurGroup(''); setSidebarOpen(false); }}>{t.uncategorized}</div>
                            <div className={`toc-item ${curGroup === '__TRASH__' ? 'active' : ''}`} onClick={() => { setCurGroup('__TRASH__'); setSidebarOpen(false); }} style={{ color: curGroup === '__TRASH__' ? 'var(--primary)' : '#ff4d4f' }}><Icon d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" size={18} style={{ marginRight: 8, verticalAlign: 'text-bottom' }} /> {t.trash}</div>
                            <div style={{ height: 1, background: 'var(--border)', margin: '5px 0' }}></div>
                            {groups.map(g => (
                                <div key={g} className={`toc-item ${curGroup === g ? 'active' : ''}`} onClick={() => { setCurGroup(g); setSidebarOpen(false); }} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                    <span>{g}</span>
                                    <button className="btn-icon" style={{ padding: 4 }} onClick={(e) => { e.stopPropagation(); setTargetGroup(g); setShowGroupMenu(true); }}><Icon d="M12 12m-1 0a1 1 0 1 0 2 0 1 1 0 1 0-2 0 M12 5m-1 0a1 1 0 1 0 2 0 1 1 0 1 0-2 0 M12 19m-1 0a1 1 0 1 0 2 0 1 1 0 1 0-2 0" size={16} /></button>
                                </div>
                            ))}
                        </div>
                        <div style={{ padding: 20 }}><button className="btn-action" style={{ width: '100%' }} onClick={() => { setShowNewGroup(true); setSidebarOpen(false); }}>+ {t.newGroup}</button></div>
                    </div>
                </div>
            )}

            {/* Common Modals */}
            {showNewGroup && <div className="modal-overlay" onClick={() => setShowNewGroup(false)}><div className="modal-content" onClick={e => e.stopPropagation()}><h4>{t.newGroup}</h4><input className="search-input" value={newGroupValue} onChange={e => setNewGroupValue(e.target.value)} placeholder={t.enterGroupName} autoFocus style={{ marginBottom: 20 }} /><div style={{ display: 'flex', gap: 10 }}><button className="modal-close" style={{ background: 'var(--surface)', color: 'var(--text)', flex: 1, marginTop: 0 }} onClick={() => setShowNewGroup(false)}>{t.cancel}</button><button className="modal-close" style={{ flex: 1, marginTop: 0 }} onClick={createGroup}>{t.ok}</button></div></div></div>}
            {showGroupMenu && targetGroup && (
                <div className="more-menu-overlay" onClick={() => { setShowGroupMenu(false); setTargetGroup(null); }}><div className="more-menu" style={{ top: 'unset', bottom: '20%', right: '20%' }} onClick={e => e.stopPropagation()}>
                    <div className="menu-item" onClick={() => { setGroupRenameValue(targetGroup); setShowGroupRename(true); setShowGroupMenu(false); }}><Icon d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7 M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" style={{ marginRight: 12 }} size={20} />{t.groupRename}</div>
                    <div className="menu-item" onClick={() => { setShowGroupDeleteConfirm(true); setShowGroupMenu(false); }} style={{ color: '#ff4d4f' }}><Icon d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" style={{ marginRight: 12 }} size={20} />{t.groupDelete}</div>
                </div></div>
            )}
            {showGroupRename && <div className="modal-overlay" onClick={() => { setShowGroupRename(false); setTargetGroup(null); }}><div className="modal-content" onClick={e => e.stopPropagation()}><h4>{t.groupRename}</h4><input className="search-input" value={groupRenameValue} onChange={e => setGroupRenameValue(e.target.value)} autoFocus style={{ marginBottom: 20 }} /><div style={{ display: 'flex', gap: 10 }}><button className="modal-close" style={{ background: 'var(--surface)', color: 'var(--text)', flex: 1, marginTop: 0 }} onClick={() => { setShowGroupRename(false); setTargetGroup(null); }}>{t.cancel}</button><button className="modal-close" style={{ flex: 1, marginTop: 0 }} onClick={confirmRenameGroup}>{t.ok}</button></div></div></div>}
            {showGroupDeleteConfirm && <div className="modal-overlay" onClick={() => { setShowGroupDeleteConfirm(false); setTargetGroup(null); }}><div className="modal-content" onClick={e => e.stopPropagation()}><h4>{t.groupDelConfirm}</h4><div style={{ display: 'flex', gap: 10, marginTop: 10 }}><button className="modal-close" style={{ background: 'var(--surface)', color: 'var(--text)', flex: 1, marginTop: 0 }} onClick={() => { setShowGroupDeleteConfirm(false); setTargetGroup(null); }}>{t.cancel}</button><button className="modal-close" style={{ background: '#ff4d4f', flex: 1, marginTop: 0 }} onClick={confirmDeleteGroup}>{t.ok}</button></div></div></div>}
            {showMoveToModal && (
                <div className="modal-overlay" onClick={() => setShowMoveToModal(false)}><div className="modal-content" onClick={e => e.stopPropagation()}>
                    <h4>{t.moveNote}</h4>
                    <div className="toc-list" style={{ maxHeight: 300, overflowY: 'auto', border: '1px solid var(--border)', borderRadius: 10, marginBottom: 20 }}>
                        <div className="toc-item" onClick={() => bulkProcess('move', '')}>{t.uncategorized}</div>
                        {groups.map(g => <div key={g} className="toc-item" onClick={() => bulkProcess('move', g)}>{g}</div>)}
                    </div>
                    <button className="modal-close" onClick={() => setShowMoveToModal(false)}>{t.cancel}</button>
                </div></div>
            )}
        </div>
    );
};

export default App;
