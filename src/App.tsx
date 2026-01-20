import React, { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import { Filesystem, Directory, Encoding } from '@capacitor/filesystem';
import { App as CapApp } from '@capacitor/app';
import { registerPlugin } from '@capacitor/core';

const OpenFolder = registerPlugin<any>('OpenFolder');
const SoraEditor = registerPlugin<any>('SoraEditor');
const DIR = 'Notes';

interface Note { id: string; title: string; content: string; time: number; isNew?: boolean; inTrash?: boolean; }

const App: React.FC = () => {
    // --- State: App Flow & Data ---
    const [view, setView] = useState<'list' | 'editor' | 'settings'>('list');
    const [notes, setNotes] = useState<Note[]>([]);
    const [groups, setGroups] = useState<string[]>([]);
    const [curGroup, setCurGroup] = useState<string | null>(null); // null=All, ''=Uncategorized
    const [curId, setCurId] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [docPath, setDocPath] = useState('...');
    const [lang, setLang] = useState<'zh' | 'en'>(() => (localStorage.getItem('lang') as any) || 'zh');
    const [theme, setTheme] = useState<'dark' | 'light'>(() => (localStorage.getItem('theme') as any) || 'dark');

    // --- State: UI Elements ---
    const [sidebarOpen, setSidebarOpen] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [renameValue, setRenameValue] = useState('');
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

    // --- State: Scrollbars ---
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
            undo: '撤销', autoSave: '自动保存', lineNum: '显示行号', saveConfirm: '是否保存当前修改？', save: '保存', discard: '放弃'
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
            undo: 'Undo', autoSave: 'Auto Save', lineNum: 'Line Numbers', saveConfirm: 'Save changes?', save: 'Save', discard: 'Discard'
        }
    }[lang]);

    useEffect(() => {
        document.documentElement.setAttribute('data-theme', theme);
        localStorage.setItem('theme', theme);
        localStorage.setItem('lang', lang);
    }, [theme, lang]);

    // --- File Operations ---
    const reloadNotes = useCallback(async () => {
        try {
            await Filesystem.mkdir({ path: DIR, directory: Directory.Documents, recursive: true }).catch(() => { });
            await Filesystem.mkdir({ path: `${DIR}/.trash`, directory: Directory.Documents, recursive: true }).catch(() => { });
            const { files } = await Filesystem.readdir({ path: DIR, directory: Directory.Documents });

            const dirs = files.filter(f => f.type === 'directory' && f.name !== '.trash').map(d => d.name);
            setGroups(dirs);

            const loadFile = async (name: string, folder = '', isTrash = false) => {
                const path = folder ? `${DIR}/${folder}/${name}` : `${DIR}/${name}`;
                const { data } = await Filesystem.readFile({ path, directory: Directory.Documents, encoding: Encoding.UTF8 });
                return { id: folder ? `${folder}/${name}` : name, title: name, content: data as string, time: Date.now(), inTrash: isTrash };
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
                if (status.publicStorage !== 'granted') {
                    await Filesystem.requestPermissions();
                }
                await OpenFolder.requestAllFilesAccess();
            } catch (e) { console.error('Permission check failed', e); }
        };
        checkPerms();
        reloadNotes();
    }, [reloadNotes]);

    const saveToDisk = useCallback(async (id: string, content: string) => {
        try {
            await Filesystem.writeFile({ path: `${DIR}/${id}`, data: content, directory: Directory.Documents, encoding: Encoding.UTF8 });
            setNotes(prev => prev.map(n => n.id === id ? { ...n, content, time: Date.now() } : n));
        } catch (e) { }
    }, []);

    const handleOpenNote = (note: Note) => {
        const path = `${docPath.replace('file://', '')}/${note.id}`;
        SoraEditor.open({
            path,
            content: note.content,
            title: note.title
        }).then((res: any) => {
            if (res && res.content !== undefined) {
                reloadNotes();
            }
        }).catch((err: any) => {
            // If cancelled, just reload in case external changes happened
            reloadNotes();
        });
    };

    const createNewNote = async () => {
        const prefix = (curGroup && curGroup !== '') ? `${curGroup}/` : '';
        const id = `${prefix}NewNote_${Date.now()}.txt`;
        const path = `${docPath.replace('file://', '')}/${id}`;

        // Create the empty file first so SoraEditor can save to it
        try {
            await Filesystem.writeFile({
                path: `${DIR}/${id}`,
                data: '',
                directory: Directory.Documents,
                encoding: Encoding.UTF8,
                recursive: true
            });

            SoraEditor.open({
                path,
                content: '',
                title: t.newNote
            }).then((res: any) => {
                reloadNotes();
            }).catch(() => reloadNotes());
        } catch (e) {
            console.error('Failed to create new note', e);
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

    useEffect(() => {
        const sub = CapApp.addListener('backButton', () => {
            if (view === 'settings' || sidebarOpen) { setView('list'); setSidebarOpen(false); }
            else CapApp.exitApp();
        });
        return () => { sub.then(h => h.remove()); };
    }, [view, sidebarOpen]);

    // --- Search & Find & UI Logic ---
    const filteredNotes = useMemo(() => {
        const q = searchQuery.toLowerCase().trim();
        let list = notes;
        if (curGroup === '__TRASH__') {
            list = list.filter(n => n.inTrash);
        } else {
            list = list.filter(n => !n.inTrash);
            if (curGroup !== null) {
                list = list.filter(n => curGroup === '' ? !n.id.includes('/') : n.id.startsWith(curGroup + '/'));
            }
        }
        return q ? list.filter(n => n.title.toLowerCase().includes(q) || n.content.toLowerCase().includes(q)) : list;
    }, [notes, searchQuery, curGroup]);

    const confirmRename = async () => {
        if (!curId || !renameValue || renameValue === curId) { return; }
        const final = renameValue.trim().endsWith('.txt') ? renameValue.trim() : renameValue.trim() + '.txt';
        try {
            const { data } = await Filesystem.readFile({ path: `${DIR}/${curId}`, directory: Directory.Documents, encoding: Encoding.UTF8 });
            await Filesystem.writeFile({ path: `${DIR}/${final}`, data: data as string, directory: Directory.Documents, encoding: Encoding.UTF8 });
            await Filesystem.deleteFile({ path: `${DIR}/${curId}`, directory: Directory.Documents });
            setCurId(final); reloadNotes();
        } catch (e) { alert(e); }
    };

    const confirmDelete = async () => {
        if (!curId) return;
        try {
            if (curNote?.inTrash) {
                await Filesystem.deleteFile({ path: `${DIR}/${curId}`, directory: Directory.Documents });
            } else {
                const name = curId.split('/').pop();
                const newPath = `.trash/${name}`;
                await Filesystem.rename({ from: `${DIR}/${curId}`, to: `${DIR}/${newPath}`, directory: Directory.Documents });
            }
            setView('list'); setCurId(null); reloadNotes();
        } catch (e) { alert(e); }
        setShowDeleteConfirm(false);
    };

    const recoverNote = async () => {
        if (!curId || !curNote?.inTrash) return;
        try {
            const name = curId.split('/').pop();
            await Filesystem.rename({ from: `${DIR}/${curId}`, to: `${DIR}/${name}`, directory: Directory.Documents }).catch(async () => {
                // Fallback if exists: rename with timestamp
                await Filesystem.rename({ from: `${DIR}/${curId}`, to: `${DIR}/restored_${Date.now()}_${name}`, directory: Directory.Documents });
            });
            setView('list'); setCurId(null); reloadNotes();
        } catch (e) { alert(e); }
    };

    const bulkProcess = async (action: 'trash' | 'delete' | 'move', target?: string) => {
        try {
            await Promise.all(selectedIds.map(async id => {
                const name = id.split('/').pop()!;
                if (action === 'trash') {
                    await Filesystem.rename({ from: `${DIR}/${id}`, to: `${DIR}/.trash/${name}`, directory: Directory.Documents }).catch(() => { });
                } else if (action === 'delete') {
                    await Filesystem.deleteFile({ path: `${DIR}/${id}`, directory: Directory.Documents });
                } else if (action === 'move') {
                    const newPath = target === '' ? name : `${target}/${name}`;
                    if (id !== newPath) await Filesystem.rename({ from: `${DIR}/${id}`, to: `${DIR}/${newPath}`, directory: Directory.Documents });
                }
            }));
            setIsSelectMode(false); setSelectedIds([]); reloadNotes();
        } catch (e) { alert(e); }
    };

    const syncScroll = (type: 'list' | 'toc') => {
        const el = { list: listRef, toc: tocListRef }[type] as any;
        const state = { list: listScroll, toc: tocScroll }[type] as any;
        const setter = { list: setListScroll, toc: setTocScroll }[type] as any;
        const currentEl = el.current;
        if (!currentEl || drag.current.active) return;
        const ratio = currentEl.scrollTop / (currentEl.scrollHeight - currentEl.clientHeight || 1);
        setter({ ...state, top: ratio * (currentEl.clientHeight - state.height), active: true });
        window.clearTimeout(timeouts.current[type]);
        timeouts.current[type] = window.setTimeout(() => setter((s: any) => ({ ...s, active: false })), 1500);
    };

    const updateScrollHeights = useCallback(() => {
        const update = (type: 'list' | 'toc', ref: React.RefObject<HTMLDivElement | null>, setter: any) => {
            if (!ref.current) return;
            const { scrollHeight, clientHeight } = ref.current;
            setter((s: any) => ({ ...s, height: Math.max((clientHeight / (scrollHeight || 1)) * clientHeight, 40) }));
        };
        update('list', listRef, setListScroll); update('toc', tocListRef, setTocScroll);
    }, []);

    useEffect(() => { updateScrollHeights(); }, [filteredNotes, view, curId, updateScrollHeights]);

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

    const curNote = useMemo(() => notes.find(n => n.id === curId), [notes, curId]);
    const Icon = ({ d, size = 24, color = "currentColor", style = {} }: { d: string, size?: number, color?: string, style?: any }) => <svg width={size} height={size} fill="none" stroke={color} strokeWidth="2" viewBox="0 0 24 24" style={style}><path d={d} /></svg>;

    if (isLoading) return null;

    return (
        <div className="app app-ready">
            {/* List View */}
            <div className={`view ${view === 'list' ? '' : 'view-hidden'}`}>
                <header>
                    <div style={{ display: 'flex', alignItems: 'center' }}>
                        {isSelectMode ? (
                            <button className="btn-icon" onClick={() => { setIsSelectMode(false); setSelectedIds([]); }}><Icon d="M6 18L18 6M6 6l12 12" /></button>
                        ) : (
                            <button className="btn-icon" onClick={() => setSidebarOpen(true)} style={{ marginRight: 10 }}><Icon d="M4 6h16M4 12h16M4 18h16" /></button>
                        )}
                        <h1>{isSelectMode ? t.selectItems.replace('{0}', selectedIds.length.toString()) : (curGroup ? curGroup : (curGroup === '' ? t.uncategorized : t.allNotes))}</h1>
                    </div>
                    {!isSelectMode && <button className="btn-icon" onClick={() => setView('settings')}><Icon d="M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z" /></button>}
                </header>
                <div className="search-bar-container"><input className="search-input" placeholder={t.search} value={searchQuery} onChange={e => setSearchQuery(e.target.value)} /></div>
                <div className="list-container" ref={listRef} onScroll={() => syncScroll('list')}>
                    {filteredNotes.length === 0 ? <div style={{ textAlign: 'center', padding: 40, color: 'var(--text-dim)' }}>{t.noNotes}</div> :
                        filteredNotes.map(n => {
                            const isSel = selectedIds.includes(n.id);
                            return (
                                <div key={n.id} className={`note-card ${isSel ? 'selected' : ''}`}
                                    onClick={() => {
                                        if (isSelectMode) setSelectedIds(p => isSel ? p.filter(x => x !== n.id) : [...p, n.id]);
                                        else { handleOpenNote(n); }
                                    }}
                                    onTouchStart={() => {
                                        if (!isSelectMode) timeouts.current.lp = window.setTimeout(() => { setIsSelectMode(true); setSelectedIds([n.id]); }, 600);
                                    }}
                                    onTouchEnd={() => clearTimeout(timeouts.current.lp)}
                                    onContextMenu={e => e.preventDefault()}
                                >
                                    {isSelectMode && <div className="checkbox"><Icon d={isSel ? "M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" : "M12 12m-9 0a9 9 0 1 0 18 0 9 9 0 1 0-18 0"} /></div>}
                                    <div className="note-title">{n.title}</div>
                                    <div className="note-desc">{n.content.substring(0, 40) || '...'}</div>
                                    <div className="note-time">{new Date(n.time).toLocaleString([], { hour: '2-digit', minute: '2-digit', year: 'numeric', month: '2-digit', day: '2-digit' })}</div>
                                </div>
                            );
                        })}
                </div>
                <div className={`custom-scrollbar ${listScroll.active ? 'visible' : ''}`}><div className="scrollbar-thumb" style={{ top: listScroll.top, height: listScroll.height }} onMouseDown={e => handleDrag(e, 'list')} onTouchStart={e => handleDrag(e, 'list')} /></div>
                {!isSelectMode ? <button id="fab" onClick={createNewNote}>+</button> : (
                    <div className="selection-toolbar">
                        <button onClick={() => setSelectedIds(selectedIds.length === filteredNotes.length ? [] : filteredNotes.map(n => n.id))}>{selectedIds.length === filteredNotes.length ? t.deselectAll : t.selectAll}</button>
                        <button onClick={() => setShowMoveToModal(true)} disabled={!selectedIds.length}>{t.moveTo}</button>
                        <button style={{ color: '#ff4d4f' }} onClick={() => bulkProcess(curGroup === '__TRASH__' ? 'delete' : 'trash')} disabled={!selectedIds.length}>{curGroup === '__TRASH__' ? t.batchDelete : t.batchTrash}</button>
                    </div>
                )}
                {sidebarOpen && (
                    <div className="toc-overlay" onClick={() => setSidebarOpen(false)}>
                        <div className="toc-sidebar" onClick={e => e.stopPropagation()}>
                            <div className="toc-header"><h3>{t.groups}</h3><button className="btn-icon" onClick={() => setSidebarOpen(false)}>✕</button></div>
                            <div className="toc-list" ref={tocListRef} onScroll={() => syncScroll('toc')}>
                                <div className={`toc-item ${curGroup === null ? 'active' : ''}`} onClick={() => { setCurGroup(null); setSidebarOpen(false); }}>{t.allNotes}</div>
                                <div className={`toc-item ${curGroup === '' ? 'active' : ''}`} onClick={() => { setCurGroup(''); setSidebarOpen(false); }}>{t.uncategorized}</div>
                                <div className={`toc-item ${curGroup === '__TRASH__' ? 'active' : ''}`} onClick={() => { setCurGroup('__TRASH__'); setSidebarOpen(false); }} style={{ color: curGroup === '__TRASH__' ? 'var(--primary)' : '#ff4d4f' }}><Icon d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" size={18} style={{ marginRight: 8, verticalAlign: 'text-bottom' }} /> {t.trash}</div>
                                <div style={{ height: 1, background: 'var(--border)', margin: '5px 0' }}></div>
                                {groups.map(g => (
                                    <div key={g} className={`toc-item ${curGroup === g ? 'active' : ''}`} onClick={() => { setCurGroup(g); setSidebarOpen(false); }} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                        <span>{g}</span>
                                        <button className="btn-icon" style={{ padding: 4 }} onClick={(e) => { e.stopPropagation(); setTargetGroup(g); setShowGroupMenu(true); }}>
                                            <Icon d="M12 12m-1 0a1 1 0 1 0 2 0 1 1 0 1 0-2 0 M12 5m-1 0a1 1 0 1 0 2 0 1 1 0 1 0-2 0 M12 19m-1 0a1 1 0 1 0 2 0 1 1 0 1 0-2 0" size={16} />
                                        </button>
                                    </div>
                                ))}
                            </div>
                            <div className={`custom-scrollbar ${tocScroll.active ? 'visible' : ''}`} style={{ top: 60, bottom: 80 }}><div className="scrollbar-thumb" style={{ top: tocScroll.top, height: tocScroll.height }} onMouseDown={e => handleDrag(e, 'toc')} onTouchStart={e => handleDrag(e, 'toc')} /></div>
                            <div style={{ padding: 20 }}><button className="btn-action" style={{ width: '100%' }} onClick={() => { setShowNewGroup(true); setSidebarOpen(false); }}>+ {t.newGroup}</button></div>
                        </div>
                    </div>
                )}
            </div>

            {/* SoraEditor is handled by native plugin */}

            {/* Settings View */}
            <div className={`view ${view === 'settings' ? '' : 'view-hidden'}`}>
                <header><button className="btn-icon" onClick={() => setView('list')}>←</button><h1>{t.settings}</h1><div style={{ width: 44 }}></div></header>
                <div className="settings-content">
                    <div className="settings-row"><span>{t.theme}</span><button className="theme-btn" onClick={() => setTheme(th => th === 'dark' ? 'light' : 'dark')}>{theme === 'dark' ? t.light : t.dark}</button></div>
                    <div className="settings-row"><span>{t.lang}</span><button className="theme-btn" onClick={() => setLang(l => l === 'zh' ? 'en' : 'zh')}>{lang === 'zh' ? 'English' : '中文'}</button></div>
                    <div className="settings-row" onClick={() => window.open('https://github.com/abc15018045126/notes', '_blank')} style={{ cursor: 'pointer' }}><span>{t.repo}</span><Icon d="M12 .3a12 12 0 0 0-3.8 23.4c.6.1.8-.3.8-.6v-2c-3.3.7-4-1.4-4-1.4-.6-1.4-1.4-1.8-1.4-1.8-1-.7.1-.7.1-.7 1.2.1 1.9 1.2 1.9 1.2 1 1.8 2.8 1.3 3.5 1 .1-.8.4-1.3.8-1.6-2.7-.3-5.5-1.3-5.5-6 0-1.3.5-2.4 1.2-3.2-.1-.3-.5-1.5.1-3.2 0 0 1-.3 3.3 1.2 1-.3 2-.4 3-.4s2 .1 3 .4c2.3-1.5 3.3-1.2 3.3-1.2.7 1.7.2 2.9.1 3.2.8.8 1.2 1.9 1.2 3.2 0 4.6-2.8 5.6-5.5 5.9.4.4.8 1.1.8 2.2V23c0 .3.2.7.8.6A12 12 0 0 0 12 .3" size={20} /></div>
                    <span className="path-label">{t.path}</span><div className="path-text" style={{ cursor: 'pointer', border: '1px solid var(--primary)', marginTop: 5 }} onClick={async () => { try { await OpenFolder.open(); } catch (e) { alert('Error: ' + e); } }}>{docPath}</div>
                </div>
            </div>

            {/* Modals */}
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
            {/* Deprecated modals removed */}

            <style>{`
                :root { --primary: #fff; --primary-rgb: 255,255,255; --bg: #000; --surface: #121212; --text: #fff; --text-dim: #888; --border: #222; }
                [data-theme='light'] { --primary: #000; --primary-rgb: 0,0,0; --bg: #fff; --surface: #f5f5f5; --text: #000; --text-dim: #666; --border: #ddd; }
                * { box-sizing: border-box; -webkit-tap-highlight-color: transparent; }
                body, html { margin: 0; width: 100%; height: 100%; font-family: 'Inter', system-ui, sans-serif; overflow: hidden; background: var(--bg); color: var(--text); }
                .app { height: 100%; width: 100%; position: relative; }
                .app-ready { animation: entrance 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.2) forwards; }
                @keyframes entrance { from { opacity: 0; transform: scale(0.95) translateY(10px); } to { opacity: 1; transform: scale(1) translateY(0); } }
                header { padding: calc(15px + env(safe-area-inset-top)) 20px 15px; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid var(--border); }
                header h1 { margin: 0; font-size: 1.2rem; }
                .view { position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: var(--bg); display: flex; flex-direction: column; transition: transform 0.2s; z-index: 10; }
                .view-hidden { transform: translateX(100%); pointer-events: none; }
                .list-container { flex: 1; overflow-y: scroll; padding: 10px 15px 120px; scrollbar-width: none; }
                .list-container::-webkit-scrollbar, #editor-area::-webkit-scrollbar { display: none; }
                .custom-scrollbar { position: absolute; right: 2px; top: 130px; bottom: 0; width: 30px; z-index: 110; pointer-events: none; opacity: 0; transition: opacity 0.3s; }
                .custom-scrollbar.visible { opacity: 1; }
                .scrollbar-thumb { position: absolute; right: 4px; width: 6px; background: #ccc; border-radius: 3px; pointer-events: auto; touch-action: none; }
                .scrollbar-thumb::after { content: ""; position: absolute; inset: -10px -5px -10px -20px; }
                .note-card { background: var(--surface); padding: 18px; border-radius: 14px; margin-bottom: 12px; border: 1px solid var(--border); position: relative; transition: background .2s; }
                .note-card.selected { border-color: var(--primary); background: rgba(var(--primary-rgb), .05); }
                .note-card:active { opacity: 0.6; }
                .checkbox { position: absolute; right: 18px; top: 18px; color: var(--primary); }
                .note-title { font-weight: 700; padding-right: 30px; }
                .note-desc, .note-time { color: var(--text-dim); font-size: 0.85rem; }
                .note-time { font-size: 0.7rem; text-align: right; margin-top: 8px; font-style: italic; }
                .btn-icon { padding: 10px; background: transparent; border: none; color: var(--text); display: flex; cursor: pointer; }
                #fab { position: fixed; bottom: calc(30px + env(safe-area-inset-bottom)); right: 25px; width: 64px; height: 64px; border-radius: 32px; background: #fff9c4; color: #5d4037; border: none; font-size: 32px; box-shadow: 0 4px 12px rgba(0,0,0,0.2); display: flex; align-items: center; justify-content: center; z-index: 100; }
                .selection-toolbar { position: fixed; bottom: calc(20px + env(safe-area-inset-bottom)); left: 20px; right: 20px; background: var(--surface); border-radius: 16px; border: 1px solid var(--border); display: flex; box-shadow: 0 8px 30px rgba(0,0,0,0.5); z-index: 120; animation: slideUp .3s; }
                @keyframes slideUp { from { transform: translateY(100px); } }
                .selection-toolbar button { flex: 1; padding: 15px; background: transparent; border: none; color: var(--text); font-weight: 600; font-size: .9rem; border-right: 1px solid var(--border); }
                .selection-toolbar button:last-child { border-right: none; }
                .selection-toolbar button:disabled { opacity: 0.3; }
                .settings-content { padding: 20px; }
                .settings-row { display: flex; justify-content: space-between; align-items: center; padding: 15px; background: var(--surface); border-radius: 12px; margin-bottom: 20px; border: 1px solid var(--border); }
                .path-label { font-size: 0.8rem; color: var(--text-dim); margin: 0 0 10px; display: block; }
                .path-text { background: rgba(128,128,128,.1); padding: 12px; border-radius: 8px; font-family: monospace; font-size: .75rem; color: var(--primary); word-break: break-all; }
                .theme-btn { background: var(--primary); color: var(--bg); border: none; padding: 8px 16px; border-radius: 8px; font-weight: 600; }
                .theme-btn.btn-dim { background: var(--surface); color: var(--text-dim); border: 1px solid var(--border); }
                .search-bar-container { padding: 10px 15px; border-bottom: 1px solid var(--border); }
                .search-input { width: 100%; padding: 10px 15px; border-radius: 10px; border: 1px solid var(--border); background: var(--surface); color: var(--text); font-size: .95rem; outline: none; }
                .btn-small { background: transparent; border: 1px solid var(--border); color: var(--text); border-radius: 6px; padding: 5px 8px; font-size: .8rem; flex-shrink: 0; }
                .toc-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.5); z-index: 200; backdrop-filter: blur(2px); }
                .toc-sidebar { width: 280px; height: 100%; background: var(--bg); display: flex; flex-direction: column; animation: slideIn 0.3s ease-out; }
                @keyframes slideIn { from { transform: translateX(-100%); } to { transform: translateX(0); } }
                .toc-header { padding: 20px; border-bottom: 1px solid var(--border); display: flex; justify-content: space-between; align-items: center; }
                .toc-list { flex: 1; overflow-y: auto; padding: 10px 0; }
                .toc-item { padding: 15px 20px; border-bottom: 1px solid var(--border); color: var(--text); }
                .toc-item.active { background: rgba(var(--primary-rgb), .1); border-left: 4px solid var(--primary); font-weight: 700; color: var(--primary); }
                .more-menu-overlay { position: fixed; inset: 0; z-index: 150; }
                .more-menu { position: absolute; top: calc(55px + env(safe-area-inset-top)); right: 15px; width: 180px; background: var(--surface); border-radius: 12px; border: 1px solid var(--border); padding: 8px 0; animation: menuFade 0.2s ease-out; }
                @keyframes menuFade { from { opacity: 0; transform: translateY(-10px) scale(.95); } to { opacity: 1; transform: translateY(0) scale(1); } }
                .menu-item { padding: 12px 18px; display: flex; align-items: center; color: var(--text); cursor: pointer; }
                .modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.7); display: flex; align-items: center; justify-content: center; z-index: 300; padding: 20px; backdrop-filter: blur(4px); }
                .modal-content { background: var(--surface); width: 100%; max-width: 320px; border-radius: 20px; padding: 25px; border: 1px solid var(--border); animation: modalIn 0.3s cubic-bezier(.34, 1.56, .64, 1); }
                @keyframes modalIn { from { transform: scale(.8); opacity: 0; } to { transform: scale(1); opacity: 1; } }
                .modal-content h4 { margin: 0 0 20px; text-align: center; }
                .prop-row { display: flex; justify-content: space-between; margin-bottom: 12px; font-size: .9rem; color: var(--text-dim); }
                .prop-row span { font-weight: 600; color: var(--text); }
                .modal-close { width: 100%; margin-top: 20px; background: var(--primary); color: var(--bg); border: none; padding: 12px; border-radius: 10px; font-weight: 700; }
                .btn-action { background: var(--primary); color: var(--bg); border: none; border-radius: 6px; padding: 10px; font-size: 1rem; font-weight: 600; cursor: pointer; }
            `}</style>
        </div>
    );
};

export default App;
