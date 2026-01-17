import React, { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import { Filesystem, Directory, Encoding } from '@capacitor/filesystem';
import { App as CapApp } from '@capacitor/app';
import { registerPlugin } from '@capacitor/core';

const OpenFolder = registerPlugin<any>('OpenFolder');
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
    const [propInfo, setPropInfo] = useState({ lines: 0, cursorLine: 0, chapter: '' });
    const [curChapterIndex, setCurChapterIndex] = useState(0);
    const [lastEditorPos, setLastEditorPos] = useState(0);
    const [fontSize, setFontSize] = useState<number>(() => Number(localStorage.getItem('fontSize')) || 18);
    const [editorBg, setEditorBg] = useState<string>(() => localStorage.getItem('editorBg') || 'default');
    const [showEditorSettings, setShowEditorSettings] = useState(false);
    const [autoSave, setAutoSave] = useState<boolean>(() => localStorage.getItem('autoSave') !== 'false');
    const [showLineNums, setShowLineNums] = useState<boolean>(() => localStorage.getItem('showLineNums') === 'true');
    const [showSaveConfirm, setShowSaveConfirm] = useState(false);

    // --- State: Scrollbars ---
    const [listScroll, setListScroll] = useState({ top: 0, height: 40, active: false });
    const [editorScroll, setEditorScroll] = useState({ top: 0, height: 40, active: false });
    const [tocScroll, setTocScroll] = useState({ top: 0, height: 40, active: false });

    // --- Refs ---
    const textareaRef = useRef<HTMLTextAreaElement>(null);
    const listRef = useRef<HTMLDivElement>(null);
    const tocListRef = useRef<HTMLDivElement>(null);
    const lineNumsRef = useRef<HTMLDivElement>(null);
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
        localStorage.setItem('fontSize', fontSize.toString());
        localStorage.setItem('editorBg', editorBg);
        localStorage.setItem('autoSave', autoSave.toString());
        localStorage.setItem('showLineNums', showLineNums.toString());
    }, [theme, lang, fontSize, editorBg, autoSave, showLineNums]);

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

    const handleInput = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
        if (!curId || !autoSave) return;
        window.clearTimeout(timeouts.current.save);
        timeouts.current.save = window.setTimeout(() => saveToDisk(curId, e.target.value), 300);
    };

    const createNewNote = () => {
        const prefix = (curGroup && curGroup !== '') ? `${curGroup}/` : '';
        const id = `${prefix}temp_${Date.now()}.txt`;
        const newNote: Note = { id, title: t.newNote, content: '', time: Date.now(), isNew: true };
        setNotes(p => [newNote, ...p]); setCurId(id); setView('editor'); setTimeout(() => textareaRef.current?.focus(), 300);
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

    const closeEditor = useCallback(async (save = true) => {
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
        if (!autoSave && save === undefined && curId && textareaRef.current) {
            const content = textareaRef.current.value;
            const original = notes.find(n => n.id === curId)?.content || '';
            if (content !== original) { setShowSaveConfirm(true); return; }
        }
        setView('list'); setCurId(null); reloadNotes(); setShowSaveConfirm(false);
    }, [curId, notes, reloadNotes, autoSave]);

    useEffect(() => {
        const sub = CapApp.addListener('backButton', () => {
            if (view === 'editor') {
                if (!autoSave) {
                    const content = textareaRef.current?.value || '';
                    const original = notes.find(n => n.id === curId)?.content || '';
                    if (content !== original) { setShowSaveConfirm(true); return; }
                }
                closeEditor();
            }
            else if (view === 'settings' || sidebarOpen) { setView('list'); setSidebarOpen(false); }
            else CapApp.exitApp();
        });
        return () => { sub.then(h => h.remove()); };
    }, [view, closeEditor, sidebarOpen, autoSave, notes, curId]);

    // --- Search & Find & UI Logic ---
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
        ta.scrollTop = (pos / (ta.value.length || 1)) * ta.scrollHeight - 20;
    };

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

    const handleShowProps = () => {
        const ta = textareaRef.current; if (!ta) return;
        const txt = ta.value, sel = ta.selectionStart;
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
            setCurId(final); reloadNotes();
        } catch (e) { alert(e); }
        setShowRename(false);
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

    const syncScroll = (type: 'list' | 'editor' | 'toc') => {
        const el = { list: listRef, editor: textareaRef, toc: tocListRef }[type].current;
        const state = { list: listScroll, editor: editorScroll, toc: tocScroll }[type];
        const setter = { list: setListScroll, editor: setEditorScroll, toc: setTocScroll }[type];
        if (!el || drag.current.active) return;
        if (type === 'editor' && lineNumsRef.current) lineNumsRef.current.scrollTop = el.scrollTop;
        const ratio = el.scrollTop / (el.scrollHeight - el.clientHeight || 1);
        setter({ ...state, top: ratio * (el.clientHeight - state.height), active: true });
        window.clearTimeout(timeouts.current[type]);
        timeouts.current[type] = window.setTimeout(() => setter(s => ({ ...s, active: false })), 1500);
    };

    const updateScrollHeights = useCallback(() => {
        const update = (type: 'list' | 'editor' | 'toc', ref: React.RefObject<HTMLElement | null>, setter: any) => {
            if (!ref.current) return;
            const { scrollHeight, clientHeight } = ref.current;
            setter((s: any) => ({ ...s, height: Math.max((clientHeight / (scrollHeight || 1)) * clientHeight, 40) }));
        };
        update('list', listRef, setListScroll); update('editor', textareaRef, setEditorScroll); update('toc', tocListRef, setTocScroll);
    }, []);

    useEffect(() => { updateScrollHeights(); }, [filteredNotes, view, curId, tocOpen, updateScrollHeights]);

    const handleDrag = (e: React.MouseEvent | React.TouchEvent, type: 'list' | 'editor' | 'toc') => {
        const el = { list: listRef, editor: textareaRef, toc: tocListRef }[type].current;
        const setter = { list: setListScroll, editor: setEditorScroll, toc: setTocScroll }[type];
        const state = { list: listScroll, editor: editorScroll, toc: tocScroll }[type];
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
    const chapters = useMemo(() => Array.from({ length: Math.ceil((curNote?.content.length || 0) / 2000) }, (_, i) => ({ index: i, pos: i * 2000, title: t.chapter.replace('{0}', (i + 1).toString()) })), [curNote?.content, t.chapter]);
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
                                        else { setCurId(n.id); setView('editor'); }
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

            {/* Editor View */}
            <div className={`view ${view === 'editor' ? '' : 'view-hidden'}`}>
                <header>
                    <button className="btn-icon" onClick={() => { const ta = textareaRef.current; if (!ta) return; setCurChapterIndex(Math.floor((ta.scrollTop / ta.scrollHeight) * (ta.value.length / 2000))); setTocOpen(true); }}><Icon d="M4 6h16M4 12h16M4 18h16" /></button>
                    <div style={{ display: 'flex' }}>
                        <button className="btn-icon" onClick={() => { setSearchOpen(!searchOpen); if (!searchOpen && findText) setTimeout(() => { const p = textareaRef.current?.selectionStart || 0; setLastEditorPos(p); handleFind(findText, true, p); }, 0); }}><Icon d="M21 21l-4.35-4.35M19 11a8 8 0 1 1-16 0 8 8 0 0 1 16 0z" /></button>
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
                    <textarea key={curId || 'none'} ref={textareaRef} id="editor-area" placeholder={t.placeholder} defaultValue={curNote?.content || ''}
                        style={{ fontSize: `${fontSize}px`, backgroundColor: editorBg === 'default' ? 'transparent' : editorBg, color: editorBg === '#000000' ? '#ffffff' : (editorBg === 'default' ? 'var(--text)' : '#000000'), paddingLeft: showLineNums ? 60 : 20 }}
                        onChange={e => { handleInput(e); updateScrollHeights(); }} onScroll={() => syncScroll('editor')} />
                    {showLineNums && (
                        <div id="line-nums" ref={lineNumsRef} style={{ position: 'absolute', left: 0, top: 0, bottom: 0, width: 50, background: 'rgba(128,128,128,0.05)', color: 'var(--text-dim)', fontSize: fontSize * 0.7, padding: '20px 5px', textAlign: 'right', pointerEvents: 'none', lineHeight: 1.6, overflow: 'hidden', borderRight: '1px solid var(--border)' }}>
                            {(textareaRef.current?.value || curNote?.content || '').split('\n').map((_, i) => <div key={i}>{i + 1}</div>)}
                        </div>
                    )}
                    <div className={`custom-scrollbar ${editorScroll.active ? 'visible' : ''}`} style={{ top: 0, right: 2 }}><div className="scrollbar-thumb" style={{ top: editorScroll.top, height: editorScroll.height }} onMouseDown={e => handleDrag(e, 'editor')} onTouchStart={e => handleDrag(e, 'editor')} /></div>
                </div>
                {tocOpen && <div className="toc-overlay" onClick={() => setTocOpen(false)}><div className="toc-sidebar" onClick={e => e.stopPropagation()}><div className="toc-header"><h3>{t.toc}</h3><button className="btn-icon" onClick={() => setTocOpen(false)}>✕</button></div><div className="toc-list" ref={tocListRef} onScroll={() => syncScroll('toc')}>{chapters.map(ch => <div key={ch.index} className={`toc-item ${ch.index === curChapterIndex ? 'active' : ''}`} onClick={() => { scrollToPos(ch.pos); setTocOpen(false); }}>{ch.title}</div>)}</div><div className={`custom-scrollbar ${tocScroll.active ? 'visible' : ''}`} style={{ top: 60 }}><div className="scrollbar-thumb" style={{ top: tocScroll.top, height: tocScroll.height }} onMouseDown={e => handleDrag(e, 'toc')} onTouchStart={e => handleDrag(e, 'toc')} /></div></div></div>}


                {moreOpen && (
                    <div className="more-menu-overlay" onClick={() => setMoreOpen(false)}><div className="more-menu" onClick={e => e.stopPropagation()}>
                        <div className="menu-item" onClick={() => closeEditor()}><Icon d="M19 12H5M12 19l-7-7 7-7" style={{ marginRight: 12 }} size={20} />{t.back}</div>
                        <div className="menu-item" onClick={() => { textareaRef.current?.focus(); document.execCommand('undo'); setMoreOpen(false); }}><Icon d="M9 13l-4-4 4-4M5 9h11a4 4 0 010 8h-1" style={{ marginRight: 12 }} size={20} />{t.undo}</div>
                        {!curNote?.inTrash && <div className="menu-item" onClick={() => { setRenameValue(curNote?.id || ''); setShowRename(true); setMoreOpen(false); }}><Icon d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7 M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" style={{ marginRight: 12 }} size={20} />{t.rename}</div>}
                        {curNote?.inTrash && <div className="menu-item" onClick={() => { recoverNote(); setMoreOpen(false); }} style={{ color: '#4caf50' }}><Icon d="M9 14l-4-4 4-4" style={{ marginRight: 12 }} size={20} />{t.restore}</div>}
                        <div className="menu-item" onClick={() => { setShowDeleteConfirm(true); setMoreOpen(false); }} style={{ color: '#ff4d4f' }}><Icon d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" style={{ marginRight: 12 }} size={20} />{curNote?.inTrash ? t.permDelete : t.moveToTrash}</div>
                        <div className="menu-item" onClick={handleShowProps}><Icon d="M12 12m-9 0a9 9 0 1 0 18 0 9 9 0 1 0-18 0 M12 16v-4 M12 8h.01" style={{ marginRight: 12 }} size={20} />{t.properties}</div>
                        <div className="menu-item" onClick={() => { setShowEditorSettings(true); setMoreOpen(false); }}><Icon d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z M12 12m-3 0a3 3 0 1 0 6 0 3 3 0 1 0-6 0" style={{ marginRight: 12 }} size={20} />{t.editorSettings}</div>
                        <div className="menu-item" onClick={() => { setCurGroup(null); closeEditor(); }}><Icon d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" style={{ marginRight: 12 }} size={20} />{t.allNotes}</div>
                        <div className="menu-item" onClick={() => { setView('settings'); setMoreOpen(false); }}><Icon d="M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z" style={{ marginRight: 12 }} size={20} />{t.settings}</div>
                    </div></div>
                )}
                {showProps && curNote && <div className="modal-overlay" onClick={() => setShowProps(false)}><div className="modal-content" onClick={e => e.stopPropagation()}><h4>{t.fileInfo}</h4><div className="prop-row"><span>{t.title}:</span> {curNote.title}</div><div className="prop-row"><span>{t.chars}:</span> {curNote.content.length}</div><div className="prop-row"><span>{t.lines}:</span> {propInfo.lines}</div><div className="prop-row"><span>{t.cursorLine}:</span> {propInfo.cursorLine}</div><div className="prop-row"><span>{t.toc}:</span> {propInfo.chapter}</div><div className="prop-row"><span>{t.modified}:</span> {new Date(curNote.time).toLocaleString()}</div><button className="modal-close" onClick={() => setShowProps(false)}>{t.close}</button></div></div>}
                {showRename && <div className="modal-overlay" onClick={() => setShowRename(false)}><div className="modal-content" onClick={e => e.stopPropagation()}><h4>{t.rename}</h4><input className="search-input" value={renameValue} onChange={e => setRenameValue(e.target.value)} autoFocus style={{ marginBottom: 20 }} /><div style={{ display: 'flex', gap: 10 }}><button className="modal-close" style={{ background: 'var(--surface)', color: 'var(--text)', flex: 1, marginTop: 0 }} onClick={() => setShowRename(false)}>{t.cancel}</button><button className="modal-close" style={{ flex: 1, marginTop: 0 }} onClick={confirmRename}>{t.ok}</button></div></div></div>}
                {showDeleteConfirm && <div className="modal-overlay" onClick={() => setShowDeleteConfirm(false)}><div className="modal-content" onClick={e => e.stopPropagation()}><h4>{curNote?.inTrash ? t.trashConfirm : t.delConfirm}</h4><div style={{ display: 'flex', gap: 10, marginTop: 10 }}><button className="modal-close" style={{ background: 'var(--surface)', color: 'var(--text)', flex: 1, marginTop: 0 }} onClick={() => setShowDeleteConfirm(false)}>{t.cancel}</button><button className="modal-close" style={{ background: '#ff4d4f', flex: 1, marginTop: 0 }} onClick={confirmDelete}>{t.ok}</button></div></div></div>}
            </div>

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
            {showEditorSettings && (
                <div className="modal-overlay" onClick={() => setShowEditorSettings(false)}><div className="modal-content" onClick={e => e.stopPropagation()}>
                    <h4>{t.editorSettings}</h4>
                    <div style={{ marginBottom: 20 }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 10 }}><span>{t.fontSize}: {fontSize}px</span><button className="btn-small" onClick={() => setFontSize(18)}>{t.reset}</button></div>
                        <input type="range" min="12" max="36" value={fontSize} onChange={e => setFontSize(Number(e.target.value))} style={{ width: '100%', accentColor: 'var(--primary)' }} />
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
                    <div style={{ display: 'flex', gap: 15, marginTop: 20 }}>
                        <button className={`theme-btn ${autoSave ? '' : 'btn-dim'}`} onClick={() => setAutoSave(!autoSave)} style={{ flex: 1, fontSize: '0.8rem' }}>{t.autoSave}: {autoSave ? 'ON' : 'OFF'}</button>
                        <button className={`theme-btn ${showLineNums ? '' : 'btn-dim'}`} onClick={() => setShowLineNums(!showLineNums)} style={{ flex: 1, fontSize: '0.8rem' }}>{t.lineNum}: {showLineNums ? 'ON' : 'OFF'}</button>
                    </div>
                    <button className="modal-close" onClick={() => setShowEditorSettings(false)}>{t.close}</button>
                </div></div>
            )}
            {showSaveConfirm && (
                <div className="modal-overlay" onClick={() => setShowSaveConfirm(false)}><div className="modal-content" onClick={e => e.stopPropagation()}>
                    <h4>{t.saveConfirm}</h4>
                    <div style={{ display: 'flex', gap: 10 }}>
                        <button className="modal-close" style={{ background: 'var(--surface)', color: 'var(--text)', flex: 1, marginTop: 0 }} onClick={() => closeEditor(false)}>{t.discard}</button>
                        <button className="modal-close" style={{ flex: 1, marginTop: 0 }} onClick={() => closeEditor(true)}>{t.save}</button>
                    </div>
                </div></div>
            )}

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
                #editor-area { flex: 1; width: 100%; background: transparent; border: none; color: var(--text); font-size: 1.15rem; line-height: 1.6; padding: 20px; resize: none; outline: none; }
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
                .search-replace-panel { background: var(--surface); border-bottom: 1px solid var(--border); padding: 10px 12px; animation: slideDown 0.3s ease-out; width: 100%; overflow: hidden; }
                @keyframes slideDown { from { transform: translateY(-100%); opacity: 0; } to { transform: translateY(0); opacity: 1; } }
                .search-row { display: flex; align-items: center; gap: 6px; margin-bottom: 8px; }
                .search-input-wrapper { flex: 1; display: flex; align-items: center; background: var(--bg); border: 1px solid var(--border); border-radius: 8px; position: relative; min-width: 0; }
                .search-input-wrapper input { flex: 1; background: transparent; border: none; color: var(--text); padding: 8px; font-size: .9rem; outline: none; padding-right: 65px; min-width: 0; }
                .search-meta { position: absolute; right: 8px; font-size: .7rem; color: var(--text-dim); font-family: monospace; white-space: nowrap; }
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
