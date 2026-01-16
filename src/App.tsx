import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Filesystem, Directory, Encoding } from '@capacitor/filesystem';
import { App as CapApp } from '@capacitor/app';
import { registerPlugin } from '@capacitor/core';

const OpenFolder = registerPlugin<any>('OpenFolder');

interface Note {
    id: string;
    title: string;
    content: string;
    time: number;
    isNew?: boolean;
}

const DIR = 'QuickNotes';

const App: React.FC = () => {
    const [view, setView] = useState<'list' | 'editor' | 'settings'>('list');
    const [notes, setNotes] = useState<Note[]>([]);
    const [curId, setCurId] = useState<string | null>(null);
    const [lang, setLang] = useState<'zh' | 'en'>(
        (localStorage.getItem('lang') as 'zh' | 'en') || 'zh'
    );
    const [theme, setTheme] = useState<'dark' | 'light'>(
        (localStorage.getItem('theme') as 'dark' | 'light') || 'dark'
    );
    const [docPath, setDocPath] = useState<string>('...');
    const [searchQuery, setSearchQuery] = useState('');
    const [isLoading, setIsLoading] = useState(true);
    const textareaRef = useRef<HTMLTextAreaElement>(null);
    const listRef = useRef<HTMLDivElement>(null);
    const [listThumbTop, setListThumbTop] = useState(0);
    const [listThumbHeight, setListThumbHeight] = useState(40);
    const [listIsScrolling, setListIsScrolling] = useState(false);
    const [editorThumbTop, setEditorThumbTop] = useState(0);
    const [editorThumbHeight, setEditorThumbHeight] = useState(40);
    const [editorIsScrolling, setEditorIsScrolling] = useState(false);
    const [tocThumbTop, setTocThumbTop] = useState(0);
    const [tocThumbHeight, setTocThumbHeight] = useState(40);
    const [tocIsScrolling, setTocIsScrolling] = useState(false);
    const listScrollTimeoutRef = useRef<number | null>(null);
    const editorScrollTimeoutRef = useRef<number | null>(null);
    const tocScrollTimeoutRef = useRef<number | null>(null);
    const isDraggingRef = useRef(false);
    const startYRef = useRef(0);
    const startScrollTopRef = useRef(0);
    const [showRename, setShowRename] = useState(false);
    const [renameValue, setRenameValue] = useState('');

    const t = {
        zh: {
            title: '便签',
            search: '搜索内容...',
            noNotes: '还没有便签',
            noMatch: '没找到匹配项',
            settings: '设置',
            theme: '颜色主题',
            lang: '语言 / Lang',
            path: '数据目录 (点击打开)：',
            delConfirm: '确认删除？',
            placeholder: '写点什么...',
            dark: '深色',
            light: '浅色',
            back: '返回',
            newNote: '新便签.txt',
            repo: '开源地址 (GitHub)',
            deleteNote: '删除',
            toc: '目录',
            find: '查找',
            replace: '替换',
            replaceAll: '全部替换',
            more: '更多',
            rename: '重命名',
            properties: '属性',
            renamePrompt: '请输入新文件名',
            fileInfo: '文件信息',
            close: '关闭',
            chapter: '第 {0} 章',
            chars: '字数',
            modified: '最后修改',
            lines: '总行数',
            cursorLine: '光标所在行',
            ok: '确定',
            cancel: '取消'
        },
        en: {
            title: 'Notes',
            search: 'Search...',
            noNotes: 'No notes found',
            noMatch: 'No matches found',
            settings: 'Settings',
            theme: 'Appearance',
            lang: 'Language',
            path: 'Storage (Click to open):',
            delConfirm: 'Delete this note?',
            placeholder: 'Type here...',
            dark: 'Dark',
            light: 'Light',
            back: 'Back',
            newNote: 'New Note.txt',
            repo: 'Source Code (GitHub)',
            deleteNote: 'Delete',
            toc: 'Contents',
            find: 'Find',
            replace: 'Replace',
            replaceAll: 'Replace All',
            more: 'More',
            rename: 'Rename',
            properties: 'Properties',
            renamePrompt: 'Enter new filename',
            fileInfo: 'File Info',
            close: 'Close',
            chapter: 'Chapter {0}',
            chars: 'Characters',
            modified: 'Last Modified',
            lines: 'Total Lines',
            cursorLine: 'Cursor Line',
            ok: 'OK',
            cancel: 'Cancel'
        }
    }[lang];

    const [tocOpen, setTocOpen] = useState(false);
    const [searchOpen, setSearchOpen] = useState(false);
    const [moreOpen, setMoreOpen] = useState(false);
    const [findText, setFindText] = useState('');
    const [replaceText, setReplaceText] = useState('');
    const [matchIndex, setMatchIndex] = useState(-1);
    const [matches, setMatches] = useState<number[]>([]);
    const [showProps, setShowProps] = useState(false);
    const [propInfo, setPropInfo] = useState({ lines: 0, cursorLine: 0, chapter: '' });
    const [curChapterIndex, setCurChapterIndex] = useState(0);
    const [lastEditorPos, setLastEditorPos] = useState(0);
    const tocListRef = useRef<HTMLDivElement>(null);

    const openToc = () => {
        const textarea = textareaRef.current;
        if (!textarea) return;
        const ratio = textarea.scrollTop / (textarea.scrollHeight - textarea.clientHeight || 1);
        const pos = ratio * textarea.value.length;
        setCurChapterIndex(Math.floor(pos / 2000));
        setTocOpen(true);
    };

    useEffect(() => {
        if (tocOpen && tocListRef.current) {
            setTimeout(() => {
                const activeItem = tocListRef.current?.querySelector('.toc-item.active');
                activeItem?.scrollIntoView({ block: 'center' });
            }, 100);
        }
    }, [tocOpen]);

    const handleShowProps = () => {
        if (!textareaRef.current) return;
        const text = textareaRef.current.value;
        const lines = text.split('\n').length;
        const selectionStart = textareaRef.current.selectionStart;
        const cursorLine = text.substring(0, selectionStart).split('\n').length;
        const chapterIdx = Math.floor(selectionStart / 2000);
        const chapterTitle = t.chapter.replace('{0}', (chapterIdx + 1).toString());
        setPropInfo({ lines, cursorLine, chapter: chapterTitle });
        setShowProps(true);
        setMoreOpen(false);
    };

    useEffect(() => {
        document.documentElement.setAttribute('data-theme', theme);
        localStorage.setItem('theme', theme);
    }, [theme]);

    useEffect(() => {
        localStorage.setItem('lang', lang);
    }, [lang]);

    const reloadNotes = useCallback(async () => {
        try {
            await Filesystem.mkdir({ path: DIR, directory: Directory.Documents, recursive: true }).catch(() => { });
            const { files } = await Filesystem.readdir({ path: DIR, directory: Directory.Documents });

            const notePromises = files
                .filter(f => f.name.endsWith('.txt'))
                .map(async f => {
                    const content = await Filesystem.readFile({
                        path: `${DIR}/${f.name}`,
                        directory: Directory.Documents,
                        encoding: Encoding.UTF8
                    });
                    return {
                        id: f.name,
                        title: f.name,
                        content: content.data as string,
                        time: (f as any).mtime || Date.now()
                    };
                });

            const loaded = await Promise.all(notePromises);
            setNotes(loaded);

            const uri = await Filesystem.getUri({ path: DIR, directory: Directory.Documents });
            setDocPath(uri.uri);
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => {
        reloadNotes();
    }, [reloadNotes]);

    const saveToDisk = useCallback(async (id: string, content: string) => {
        try {
            await Filesystem.writeFile({
                path: `${DIR}/${id}`,
                data: content,
                directory: Directory.Documents,
                encoding: Encoding.UTF8
            });
            setNotes(prev => prev.map(n => n.id === id ? { ...n, content, time: Date.now() } : n));
        } catch (e) { }
    }, []);

    const saveTimeoutRef = useRef<number | undefined>(undefined);
    const handleInput = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
        const content = e.target.value;
        if (curId) {
            window.clearTimeout(saveTimeoutRef.current);
            saveTimeoutRef.current = window.setTimeout(() => saveToDisk(curId, content), 300);
        }
    };

    const createNewNote = () => {
        const id = `temp_${Date.now()}.txt`;
        const newNote: Note = { id, title: t.newNote, content: '', time: Date.now(), isNew: true };
        setNotes(prev => [newNote, ...prev]);
        setCurId(id);
        setView('editor');
        setTimeout(() => textareaRef.current?.focus(), 300);
    };

    const openNote = (id: string) => {
        setCurId(id);
        setView('editor');
    };

    const closeEditor = useCallback(async () => {
        if (curId && textareaRef.current) {
            const content = textareaRef.current.value;
            const note = notes.find(n => n.id === curId);
            let finalId = curId;
            if (note?.isNew && content.trim()) {
                const now = new Date();
                const dateStr = `${now.getFullYear()}.${String(now.getMonth() + 1).padStart(2, '0')}.${String(now.getDate()).padStart(2, '0')}`;
                const firstLine = content.split('\n')[0].trim().substring(0, 15).replace(/[\\\/:*?"<>|]/g, '');
                if (firstLine) finalId = `${firstLine} ${dateStr}.txt`;
            }

            try {
                await Filesystem.writeFile({ path: `${DIR}/${finalId}`, data: content, directory: Directory.Documents, encoding: Encoding.UTF8 });
                if (finalId !== curId && curId.startsWith('temp_')) {
                    await Filesystem.deleteFile({ path: `${DIR}/${curId}`, directory: Directory.Documents }).catch(() => { });
                }
            } catch (e) { }
        }
        setView('list');
        setCurId(null);
        reloadNotes();
    }, [curId, notes, reloadNotes]);

    useEffect(() => {
        const backHandler = CapApp.addListener('backButton', () => {
            if (view === 'editor') {
                closeEditor();
            } else if (view === 'settings') {
                setView('list');
            } else {
                CapApp.exitApp();
            }
        });
        return () => { backHandler.then(h => h.remove()); };
    }, [view, closeEditor]);

    const curNote = notes.find(n => n.id === curId);



    const handleFind = useCallback((text: string, scrollToMatch = true, preferredPos?: number) => {
        if (!text || !textareaRef.current) {
            setMatches([]);
            setMatchIndex(-1);
            return;
        }
        const content = textareaRef.current.value;
        const newMatches: number[] = [];
        let pos = content.indexOf(text);
        while (pos !== -1) {
            newMatches.push(pos);
            pos = content.indexOf(text, pos + 1);
        }
        setMatches(newMatches);
        if (newMatches.length > 0) {
            let bestMatchIdx = 0;
            if (preferredPos !== undefined) {
                const idx = newMatches.findIndex(m => m >= preferredPos);
                if (idx !== -1) bestMatchIdx = idx;
            }
            setMatchIndex(bestMatchIdx);

            if (scrollToMatch) {
                const index = newMatches[bestMatchIdx];
                textareaRef.current.focus();
                textareaRef.current.setSelectionRange(index, index + text.length);
                scrollToPos(index);
            }
        } else {
            setMatchIndex(-1);
        }
    }, [curNote?.content]);

    const toggleSearch = (open: boolean) => {
        setSearchOpen(open);
        if (open) {
            const pos = textareaRef.current?.selectionStart || 0;
            setLastEditorPos(pos);
            if (findText) {
                setTimeout(() => handleFind(findText, true, pos), 0);
            }
        } else {
            setMatches([]);
        }
    };

    const findNext = () => {
        if (matches.length === 0) return;
        const next = (matchIndex + 1) % matches.length;
        setMatchIndex(next);
        const index = matches[next];
        textareaRef.current?.focus();
        textareaRef.current?.setSelectionRange(index, index + findText.length);
        scrollToPos(index);
    };

    const findPrev = () => {
        if (matches.length === 0) return;
        const prev = (matchIndex - 1 + matches.length) % matches.length;
        setMatchIndex(prev);
        const index = matches[prev];
        textareaRef.current?.focus();
        textareaRef.current?.setSelectionRange(index, index + findText.length);
        scrollToPos(index);
    };

    const handleReplace = () => {
        if (!textareaRef.current || matchIndex === -1) return;
        const content = textareaRef.current.value;
        const start = matches[matchIndex];
        const newContent = content.substring(0, start) + replaceText + content.substring(start + findText.length);
        textareaRef.current.value = newContent;
        handleInput({ target: { value: newContent } } as any);
        // Refresh matches without jumping
        setTimeout(() => handleFind(findText, false), 0);
    };

    const handleReplaceAll = () => {
        if (!textareaRef.current || !findText) return;
        const content = textareaRef.current.value;
        const newContent = content.split(findText).join(replaceText);
        textareaRef.current.value = newContent;
        handleInput({ target: { value: newContent } } as any);
        setTimeout(() => handleFind(findText), 0);
    };

    const scrollToPos = (pos: number) => {
        if (!textareaRef.current) return;
        const textarea = textareaRef.current;
        const total = textarea.value.length;
        if (total === 0) return;

        const scrollHeight = textarea.scrollHeight;
        const ratio = pos / total;

        textarea.scrollTop = ratio * scrollHeight - 20;
    };

    const renameNote = () => {
        if (!curId || !curNote) return;
        setRenameValue(curNote.id);
        setShowRename(true);
        setMoreOpen(false);
    };

    const confirmRename = async () => {
        if (!curId || !renameValue || renameValue === curId) {
            setShowRename(false);
            return;
        }
        const finalTitle = renameValue.trim().endsWith('.txt') ? renameValue.trim() : renameValue.trim() + '.txt';
        try {
            const { data } = await Filesystem.readFile({
                path: `${DIR}/${curId}`,
                directory: Directory.Documents,
                encoding: Encoding.UTF8
            });
            await Filesystem.writeFile({
                path: `${DIR}/${finalTitle}`,
                data: data as string,
                directory: Directory.Documents,
                encoding: Encoding.UTF8
            });
            await Filesystem.deleteFile({
                path: `${DIR}/${curId}`,
                directory: Directory.Documents
            });
            setCurId(finalTitle);
            reloadNotes();
        } catch (e) {
            alert(e);
        }
        setShowRename(false);
    };


    const chapters = React.useMemo(() => {
        const content = curNote?.content || '';
        return Array.from({ length: Math.ceil(content.length / 2000) }, (_, i) => ({
            index: i,
            pos: i * 2000,
            title: t.chapter.replace('{0}', (i + 1).toString())
        }));
    }, [curNote?.content, t.chapter]);

    const filteredNotes = React.useMemo(() => {
        const query = searchQuery.toLowerCase().trim();
        const sorted = [...notes].sort((a, b) => b.time - a.time);
        if (!query) return sorted;
        return sorted.filter(note =>
            note.title.toLowerCase().includes(query) ||
            note.content.toLowerCase().includes(query)
        );
    }, [notes, searchQuery]);

    const updateScrollbarHeights = useCallback(() => {
        if (listRef.current) {
            const { scrollHeight, clientHeight } = listRef.current;
            setListThumbHeight(Math.max((clientHeight / (scrollHeight || 1)) * clientHeight, 40));
        }
        if (textareaRef.current) {
            const { scrollHeight, clientHeight } = textareaRef.current;
            setEditorThumbHeight(Math.max((clientHeight / (scrollHeight || 1)) * clientHeight, 40));
        }
        if (tocListRef.current) {
            const { scrollHeight, clientHeight } = tocListRef.current;
            setTocThumbHeight(Math.max((clientHeight / (scrollHeight || 1)) * clientHeight, 40));
        }
    }, []);

    useEffect(() => {
        updateScrollbarHeights();
    }, [filteredNotes, view, curId, updateScrollbarHeights, tocOpen]);

    const handleListScroll = useCallback(() => {
        if (!listRef.current || isDraggingRef.current) return;
        const { scrollTop, scrollHeight, clientHeight } = listRef.current;
        if (scrollHeight <= clientHeight) return;
        const ratio = scrollTop / (scrollHeight - clientHeight);
        setListThumbTop(ratio * (clientHeight - listThumbHeight));
        setListIsScrolling(true);
        if (listScrollTimeoutRef.current) window.clearTimeout(listScrollTimeoutRef.current);
        listScrollTimeoutRef.current = window.setTimeout(() => setListIsScrolling(false), 1500);
    }, [listThumbHeight]);

    const handleEditorScroll = useCallback(() => {
        if (!textareaRef.current || isDraggingRef.current) return;
        const { scrollTop, scrollHeight, clientHeight } = textareaRef.current;
        if (scrollHeight <= clientHeight) return;
        const ratio = scrollTop / (scrollHeight - clientHeight);
        setEditorThumbTop(ratio * (clientHeight - editorThumbHeight));
        setEditorIsScrolling(true);
        if (editorScrollTimeoutRef.current) window.clearTimeout(editorScrollTimeoutRef.current);
        editorScrollTimeoutRef.current = window.setTimeout(() => setEditorIsScrolling(false), 1500);
    }, [editorThumbHeight]);

    const handleTocScroll = useCallback(() => {
        if (!tocListRef.current || isDraggingRef.current) return;
        const { scrollTop, scrollHeight, clientHeight } = tocListRef.current;
        if (scrollHeight <= clientHeight) return;
        const ratio = scrollTop / (scrollHeight - clientHeight);
        setTocThumbTop(ratio * (clientHeight - tocThumbHeight));
        setTocIsScrolling(true);
        if (tocScrollTimeoutRef.current) window.clearTimeout(tocScrollTimeoutRef.current);
        tocScrollTimeoutRef.current = window.setTimeout(() => setTocIsScrolling(false), 1500);
    }, [tocThumbHeight]);

    const handleDragStart = (e: React.MouseEvent | React.TouchEvent, type: 'list' | 'editor' | 'toc') => {
        const target = type === 'editor' ? textareaRef.current : (type === 'list' ? listRef.current : tocListRef.current);
        if (!target) return;
        isDraggingRef.current = true;
        if (type === 'editor') setEditorIsScrolling(true);
        else if (type === 'list') setListIsScrolling(true);
        else setTocIsScrolling(true);

        const clientY = 'touches' in e ? e.touches[0].clientY : e.clientY;
        startYRef.current = clientY;
        startScrollTopRef.current = target.scrollTop;

        if (type === 'editor' && editorScrollTimeoutRef.current) window.clearTimeout(editorScrollTimeoutRef.current);
        if (type === 'list' && listScrollTimeoutRef.current) window.clearTimeout(listScrollTimeoutRef.current);
        if (type === 'toc' && tocScrollTimeoutRef.current) window.clearTimeout(tocScrollTimeoutRef.current);

        const moveHandler = (moveEvent: MouseEvent | TouchEvent) => {
            if (!isDraggingRef.current || !target) return;
            moveEvent.preventDefault();
            const currentY = 'touches' in moveEvent ? moveEvent.touches[0].clientY : (moveEvent as MouseEvent).clientY;
            const deltaY = currentY - startYRef.current;
            const { scrollHeight, clientHeight } = target;
            const thumbH = type === 'editor' ? editorThumbHeight : (type === 'list' ? listThumbHeight : tocThumbHeight);
            const availableSpace = clientHeight - thumbH;
            const scrollRange = scrollHeight - clientHeight;
            if (scrollRange <= 0) return;
            const scrollDelta = (deltaY / availableSpace) * scrollRange;
            target.scrollTop = startScrollTopRef.current + scrollDelta;

            const ratio = target.scrollTop / (scrollHeight - clientHeight);
            if (type === 'editor') setEditorThumbTop(ratio * availableSpace);
            else if (type === 'list') setListThumbTop(ratio * availableSpace);
            else setTocThumbTop(ratio * availableSpace);
        };

        const endHandler = () => {
            isDraggingRef.current = false;
            if (type === 'editor') editorScrollTimeoutRef.current = window.setTimeout(() => setEditorIsScrolling(false), 1500);
            else if (type === 'list') listScrollTimeoutRef.current = window.setTimeout(() => setListIsScrolling(false), 1500);
            else tocScrollTimeoutRef.current = window.setTimeout(() => setTocIsScrolling(false), 1500);
            document.removeEventListener('mousemove', moveHandler);
            document.removeEventListener('mouseup', endHandler);
            document.removeEventListener('touchmove', moveHandler);
            document.removeEventListener('touchend', endHandler);
        };

        document.addEventListener('mousemove', moveHandler);
        document.addEventListener('mouseup', endHandler);
        document.addEventListener('touchmove', moveHandler, { passive: false });
        document.addEventListener('touchend', endHandler);
    };

    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const deleteNote = () => {
        if (!curId) return;
        setShowDeleteConfirm(true);
        setMoreOpen(false);
    };

    const confirmDelete = async () => {
        if (!curId) return;
        try {
            await Filesystem.deleteFile({ path: `${DIR}/${curId}`, directory: Directory.Documents });
            setNotes(prev => prev.filter(n => n.id !== curId));
            setView('list');
            setCurId(null);
        } catch (e) { }
        setShowDeleteConfirm(false);
    };

    if (isLoading) return null;

    return (
        <div className="app app-ready">
            <div className={`view ${view === 'list' ? '' : 'view-hidden'}`}>
                <header>
                    <h1>{t.title}</h1>
                    <button className="btn-icon" onClick={() => setView('settings')}>
                        <svg width="24" height="24" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                            <circle cx="12" cy="12" r="3" /><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0Map0-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-2 2 2 2 0 01-2-2v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83 0 2 2 0 010-2.83l.06-.06a1.65 1.65 0 00.33-1.82 1.65 1.65 0 00-1.51-1H3a2 2 0 01-2-2 2 2 0 012-2h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 010-2.83 2 2 0 012.83 0l.06.06a1.65 1.65 0 001.82.33H9a1.65 1.65 0 001-1.51V3a2 2 0 012-2 2 2 0 012 2v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 0 2 2 0 010 2.83l-.06.06a1.65 1.65 0 00-.33 1.82V9a1.65 1.65 0 001.51 1H21a2 2 0 012 2 2 2 0 01-2 2h-.09a1.65 1.65 0 00-1.51 1z" />
                        </svg>
                    </button>
                </header>
                <div className="search-bar-container">
                    <input
                        type="text"
                        className="search-input"
                        placeholder={t.search}
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                    />
                </div>
                <div className="list-container" ref={listRef} onScroll={handleListScroll}>
                    {filteredNotes.map(note => (
                        <div key={note.id} className="note-card" onClick={() => openNote(note.id)}>
                            <div className="note-title">{note.title}</div>
                            <div className="note-desc">{note.content.substring(0, 40) || '...'}</div>
                            <div className="note-time">
                                {new Date(note.time).toLocaleString([], { hour: '2-digit', minute: '2-digit', year: 'numeric', month: '2-digit', day: '2-digit' })}
                            </div>
                        </div>
                    ))}
                </div>
                <div className={`custom-scrollbar ${listIsScrolling ? 'visible' : ''}`}>
                    <div
                        className="scrollbar-thumb"
                        style={{ top: listThumbTop, height: listThumbHeight }}
                        onMouseDown={(e) => handleDragStart(e, 'list')}
                        onTouchStart={(e) => handleDragStart(e, 'list')}
                    />
                </div>
                <button id="fab" onClick={createNewNote}>+</button>
            </div>

            <div className={`view ${view === 'editor' ? '' : 'view-hidden'}`}>
                <header>
                    <div style={{ display: 'flex', alignItems: 'center' }}>
                        <button className="btn-icon" onClick={openToc}>
                            <svg width="24" height="24" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                                <path d="M4 6h16M4 12h16M4 18h16" />
                            </svg>
                        </button>
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center' }}>
                        <button className="btn-icon" onClick={() => toggleSearch(!searchOpen)}>
                            <svg width="24" height="24" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                                <circle cx="11" cy="11" r="8" /><path d="m21 21-4.3-4.3" />
                            </svg>
                        </button>
                        <button className="btn-icon" onClick={() => setMoreOpen(!moreOpen)} style={{ marginLeft: '10px' }}>
                            <svg width="24" height="24" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                                <circle cx="12" cy="5" r="1" /><circle cx="12" cy="12" r="1" /><circle cx="12" cy="19" r="1" />
                            </svg>
                        </button>
                    </div>
                </header>

                {searchOpen && (
                    <div className="search-replace-panel">
                        <div className="search-row">
                            <div className="search-input-wrapper">
                                <div onClick={() => {
                                    const pos = textareaRef.current?.selectionStart || 0;
                                    setLastEditorPos(pos);
                                    handleFind(findText, true, pos);
                                }} style={{ cursor: 'pointer', display: 'flex', alignItems: 'center' }}>
                                    <svg width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" style={{ margin: '0 8px' }}>
                                        <circle cx="11" cy="11" r="8" /><path d="m21 21-4.3-4.3" />
                                    </svg>
                                </div>
                                <input
                                    placeholder={t.find}
                                    value={findText}
                                    onChange={(e) => { setFindText(e.target.value); handleFind(e.target.value, false, lastEditorPos); }}
                                    onKeyDown={(e) => { if (e.key === 'Enter') handleFind(findText, true, lastEditorPos); }}
                                    autoFocus
                                />
                                <div className="search-meta">
                                    {matches.length > 0 ? `${matchIndex + 1}/${matches.length}` : '0/0'}
                                </div>
                            </div>
                            <button className="btn-small" onClick={findPrev}>↑</button>
                            <button className="btn-small" onClick={findNext}>↓</button>
                        </div>
                        <div className="search-row">
                            <div className="search-input-wrapper">
                                <input
                                    placeholder={t.replace}
                                    style={{ paddingLeft: '12px' }}
                                    value={replaceText}
                                    onChange={(e) => setReplaceText(e.target.value)}
                                />
                            </div>
                            <button className="btn-small" onClick={handleReplace}>{t.replace}</button>
                            <button className="btn-small" onClick={handleReplaceAll}>{t.replaceAll}</button>
                        </div>
                    </div>
                )}

                <div className="editor-container" style={{ position: 'relative', flex: 1, display: 'flex' }}>
                    <textarea
                        key={curId || 'none'}
                        ref={textareaRef}
                        id="editor-area"
                        placeholder={t.placeholder}
                        defaultValue={curNote?.content || ''}
                        onChange={(e) => { handleInput(e); updateScrollbarHeights(); }}
                        onScroll={handleEditorScroll}
                    />
                    <div className={`custom-scrollbar ${editorIsScrolling ? 'visible' : ''}`} style={{ top: 0 }}>
                        <div
                            className="scrollbar-thumb"
                            style={{ top: editorThumbTop, height: editorThumbHeight }}
                            onMouseDown={(e) => handleDragStart(e, 'editor')}
                            onTouchStart={(e) => handleDragStart(e, 'editor')}
                        />
                    </div>
                </div>

                {tocOpen && (
                    <div className="toc-overlay" onClick={() => setTocOpen(false)}>
                        <div className="toc-sidebar" onClick={e => e.stopPropagation()}>
                            <div className="toc-header">
                                <h3>{t.toc}</h3>
                                <button className="btn-icon" onClick={() => setTocOpen(false)}>✕</button>
                            </div>
                            <div className="toc-list" ref={tocListRef} onScroll={handleTocScroll}>
                                {chapters.map(ch => (
                                    <div
                                        key={ch.index}
                                        className={`toc-item ${ch.index === curChapterIndex ? 'active' : ''}`}
                                        onClick={() => { scrollToPos(ch.pos); setTocOpen(false); }}
                                    >
                                        {ch.title}
                                    </div>
                                ))}
                            </div>
                            <div className={`custom-scrollbar ${tocIsScrolling ? 'visible' : ''}`} style={{ top: '60px' }}>
                                <div
                                    className="scrollbar-thumb"
                                    style={{ top: tocThumbTop, height: tocThumbHeight }}
                                    onMouseDown={(e) => handleDragStart(e, 'toc')}
                                    onTouchStart={(e) => handleDragStart(e, 'toc')}
                                />
                            </div>
                        </div>
                    </div>
                )}

                {moreOpen && (
                    <div className="more-menu-overlay" onClick={() => setMoreOpen(false)}>
                        <div className="more-menu" onClick={e => e.stopPropagation()}>
                            <div className="menu-item" onClick={renameNote}>
                                <svg width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" style={{ marginRight: '12px' }}>
                                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" /><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
                                </svg>
                                {t.rename}
                            </div>
                            <div className="menu-item" onClick={deleteNote} style={{ color: '#ff4d4f' }}>
                                <svg width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" style={{ marginRight: '12px' }}>
                                    <path d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                                </svg>
                                {t.deleteNote || t.delConfirm.split('？')[0]}
                            </div>
                            <div className="menu-item" onClick={handleShowProps}>
                                <svg width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" style={{ marginRight: '12px' }}>
                                    <circle cx="12" cy="12" r="10" /><line x1="12" y1="16" x2="12" y2="12" /><line x1="12" y1="8" x2="12.01" y2="8" />
                                </svg>
                                {t.properties}
                            </div>
                            <div className="menu-item" onClick={() => { setView('settings'); setMoreOpen(false); }}>
                                <svg width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" style={{ marginRight: '12px' }}>
                                    <circle cx="12" cy="12" r="3" /><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z" />
                                </svg>
                                {t.settings}
                            </div>
                        </div>
                    </div>
                )}

                {showProps && curNote && (
                    <div className="modal-overlay" onClick={() => setShowProps(false)}>
                        <div className="modal-content" onClick={e => e.stopPropagation()}>
                            <h4>{t.fileInfo}</h4>
                            <div className="prop-row"><span>{t.title}:</span> {curNote.title}</div>
                            <div className="prop-row"><span>{t.chars}:</span> {curNote.content.length}</div>
                            <div className="prop-row"><span>{t.lines}:</span> {propInfo.lines}</div>
                            <div className="prop-row"><span>{t.cursorLine}:</span> {propInfo.cursorLine}</div>
                            <div className="prop-row"><span>{t.toc}:</span> {propInfo.chapter}</div>
                            <div className="prop-row"><span>{t.modified}:</span> {new Date(curNote.time).toLocaleString()}</div>
                            <button className="modal-close" onClick={() => setShowProps(false)}>{t.close}</button>
                        </div>
                    </div>
                )}

                {showRename && (
                    <div className="modal-overlay" onClick={() => setShowRename(false)}>
                        <div className="modal-content" onClick={e => e.stopPropagation()}>
                            <h4>{t.rename}</h4>
                            <input
                                className="search-input"
                                value={renameValue}
                                onChange={e => setRenameValue(e.target.value)}
                                autoFocus
                                style={{ marginBottom: '20px' }}
                            />
                            <div style={{ display: 'flex', gap: '10px' }}>
                                <button className="modal-close" style={{ background: 'var(--surface)', color: 'var(--text)', flex: 1, marginTop: 0 }} onClick={() => setShowRename(false)}>{t.cancel}</button>
                                <button className="modal-close" style={{ flex: 1, marginTop: 0 }} onClick={confirmRename}>{t.ok}</button>
                            </div>
                        </div>
                    </div>
                )}

                {showDeleteConfirm && (
                    <div className="modal-overlay" onClick={() => setShowDeleteConfirm(false)}>
                        <div className="modal-content" onClick={e => e.stopPropagation()}>
                            <h4>{t.delConfirm}</h4>
                            <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
                                <button className="modal-close" style={{ background: 'var(--surface)', color: 'var(--text)', flex: 1, marginTop: 0 }} onClick={() => setShowDeleteConfirm(false)}>{t.cancel}</button>
                                <button className="modal-close" style={{ background: '#ff4d4f', flex: 1, marginTop: 0 }} onClick={confirmDelete}>{t.ok}</button>
                            </div>
                        </div>
                    </div>
                )}
            </div>

            <div className={`view ${view === 'settings' ? '' : 'view-hidden'}`}>
                <header>
                    <button className="btn-icon" onClick={() => setView('list')}>←</button>
                    <h1>{t.settings}</h1>
                    <div style={{ width: '44px' }}></div>
                </header>
                <div className="settings-content">
                    <div className="settings-row">
                        <span>{t.theme}</span>
                        <button className="theme-btn" onClick={() => setTheme(th => th === 'dark' ? 'light' : 'dark')}>
                            {theme === 'dark' ? t.light : t.dark}
                        </button>
                    </div>
                    <div className="settings-row">
                        <span>{t.lang}</span>
                        <button className="theme-btn" onClick={() => setLang(l => l === 'zh' ? 'en' : 'zh')}>
                            {lang === 'zh' ? 'English' : '中文'}
                        </button>
                    </div>
                    <div className="settings-row" onClick={() => window.open('https://github.com/abc15018045126/notes', '_blank')} style={{ cursor: 'pointer' }}>
                        <span>{t.repo}</span>
                        <svg width="20" height="20" fill="currentColor" viewBox="0 0 24 24">
                            <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.042-1.416-4.042-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z" />
                        </svg>
                    </div>
                    <span className="path-label">{t.path}</span>
                    <div
                        className="path-text"
                        style={{ cursor: 'pointer', border: '1px solid var(--primary)', marginTop: '5px' }}
                        onClick={async () => {
                            try {
                                await OpenFolder.open();
                            } catch (e) {
                                alert('Error: ' + e);
                            }
                        }}
                    >
                        {docPath}
                    </div>
                </div>
            </div>

            <style>{`
        :root {
          --primary: #ffffff;
          --primary-rgb: 255, 255, 255;
          --bg: #000000;
          --surface: #121212;
          --text: #ffffff;
          --text-dim: #888888;
          --border: #222222;
        }
        [data-theme='light'] {
          --primary: #000000;
          --primary-rgb: 0, 0, 0;
          --bg: #ffffff;
          --surface: #f5f5f5;
          --text: #000000;
          --text-dim: #666666;
          --border: #dddddd;
        }
        * { box-sizing: border-box; -webkit-tap-highlight-color: transparent; }
        body, html { margin: 0; padding: 0; width: 100%; height: 100%; font-family: 'Inter', system-ui, sans-serif; overflow: hidden; background: var(--bg); color: var(--text); }
        .app { 
          height: 100%; width: 100%; position: relative; 
          background: var(--bg);
        }
        .app-ready { animation: appEntrance 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.2) forwards; }
        @keyframes appEntrance {
          from { opacity: 0; transform: scale(0.95) translateY(10px); }
          to { opacity: 1; transform: scale(1) translateY(0); }
        }
        header { 
          padding: calc(15px + env(safe-area-inset-top)) 20px 15px; 
          display: flex; align-items: center; justify-content: space-between; 
          border-bottom: 1px solid var(--border); background: var(--bg);
        }
        header h1 { margin: 0; font-size: 1.2rem; font-weight: 700; }
        .view { 
          position: absolute; top: 0; left: 0; width: 100%; height: 100%; 
          background: var(--bg); display: flex; flex-direction: column; transition: transform 0.2s ease-out; z-index: 10;
        }
        .view-hidden { transform: translateX(100%); pointer-events: none; }
        .list-container { flex: 1; overflow-y: scroll; padding: 10px 15px 120px; scrollbar-width: none; -ms-overflow-style: none; }
        .list-container::-webkit-scrollbar { display: none; }
        
        .custom-scrollbar { position: absolute; right: 2px; top: 130px; bottom: 0; width: 30px; z-index: 110; pointer-events: none; opacity: 0; transition: opacity 0.3s; }
        .custom-scrollbar.visible { opacity: 1; }
        .scrollbar-thumb { position: absolute; right: 4px; width: 6px; background: #cccccc; border-radius: 3px; pointer-events: auto; touch-action: none; box-shadow: 0 0 5px rgba(0,0,0,0.1); }
        .scrollbar-thumb::after { content: ""; position: absolute; top: -10px; bottom: -10px; left: -20px; right: -5px; } /* Ghost hitbox */
        .note-card { background: var(--surface); padding: 18px; border-radius: 14px; margin-bottom: 12px; border: 1px solid var(--border); }
        .note-card:active { opacity: 0.6; }
        .note-title { font-weight: 700; font-size: 1rem; margin-bottom: 4px; }
        .note-desc { font-size: 0.85rem; color: var(--text-dim); }
        .note-time { font-size: 0.7rem; color: var(--text-dim); text-align: right; margin-top: 8px; font-style: italic; }
        #editor-area { flex: 1; width: 100%; background: transparent; border: none; color: var(--text); font-size: 1.15rem; line-height: 1.6; padding: 20px; resize: none; outline: none; scrollbar-width: none; -ms-overflow-style: none; }
        #editor-area::-webkit-scrollbar { display: none; }
        .btn-icon { padding: 10px; background: transparent; border: none; color: var(--text); display: flex; cursor: pointer; }
        #fab { position: fixed; bottom: calc(30px + env(safe-area-inset-bottom)); right: 25px; width: 64px; height: 64px; border-radius: 32px; background: #fff9c4; color: #5d4037; border: none; font-size: 32px; box-shadow: 0 4px 12px rgba(0,0,0,0.2); display: flex; align-items: center; justify-content: center; z-index: 100; cursor: pointer; }
        .settings-content { padding: 20px; }
        .settings-row { display: flex; justify-content: space-between; align-items: center; padding: 15px; background: var(--surface); border-radius: 12px; margin-bottom: 20px; border: 1px solid var(--border); }
        .path-label { font-size: 0.8rem; color: var(--text-dim); margin-bottom: 10px; display: block; }
        .path-text { background: rgba(128,128,128,0.1); padding: 12px; border-radius: 8px; font-family: monospace; font-size: 0.75rem; color: var(--primary); word-break: break-all; }
        .theme-btn { background: var(--primary); color: var(--bg); border: none; padding: 8px 16px; border-radius: 8px; font-weight: 600; cursor: pointer; }
        .search-bar-container { padding: 10px 15px; background: var(--bg); border-bottom: 1px solid var(--border); }
        .search-input { width: 100%; padding: 10px 15px; border-radius: 10px; border: 1px solid var(--border); background: var(--surface); color: var(--text); font-size: 0.95rem; outline: none; }
        .search-input:focus { border-color: var(--primary); box-shadow: 0 0 0 2px rgba(var(--primary-rgb), 0.2); }

        /* Search & Replace Panel */
        .search-replace-panel { background: var(--surface); border-bottom: 1px solid var(--border); padding: 10px 12px; animation: slideDown 0.3s ease-out; width: 100%; overflow: hidden; }
        @keyframes slideDown { from { transform: translateY(-100%); opacity: 0; } to { transform: translateY(0); opacity: 1; } }
        .search-row { display: flex; align-items: center; gap: 6px; margin-bottom: 8px; width: 100%; }
        .search-row:last-child { margin-bottom: 0; }
        .search-input-wrapper { flex: 1; display: flex; align-items: center; background: var(--bg); border: 1px solid var(--border); border-radius: 8px; position: relative; min-width: 0; }
        .search-input-wrapper input { flex: 1; background: transparent; border: none; color: var(--text); padding: 8px 8px; font-size: 0.9rem; outline: none; padding-right: 65px; min-width: 0; }
        .search-meta { position: absolute; right: 8px; font-size: 0.7rem; color: var(--text-dim); font-family: monospace; pointer-events: none; white-space: nowrap; }
        .btn-small { background: transparent; border: 1px solid var(--border); color: var(--text); border-radius: 6px; padding: 5px 8px; font-size: 0.8rem; cursor: pointer; flex-shrink: 0; white-space: nowrap; }
        .btn-small:active { background: var(--bg); }
        .btn-action { background: var(--primary); color: var(--bg); border: none; border-radius: 6px; padding: 6px 14px; font-size: 0.85rem; font-weight: 600; cursor: pointer; }

        /* TOC Sidebar */
        .toc-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); z-index: 200; backdrop-filter: blur(2px); }
        .toc-sidebar { width: 280px; height: 100%; background: var(--bg); box-shadow: 4px 0 12px rgba(0,0,0,0.3); display: flex; flex-direction: column; animation: slideIn 0.3s ease-out; }
        @keyframes slideIn { from { transform: translateX(-100%); } to { transform: translateX(0); } }
        .toc-header { padding: 20px; border-bottom: 1px solid var(--border); display: flex; justify-content: space-between; align-items: center; }
        .toc-list { flex: 1; overflow-y: auto; padding: 10px 0; }
        .toc-item { padding: 15px 20px; border-bottom: 1px solid var(--border); cursor: pointer; font-size: 1rem; color: var(--text); }
        .toc-item.active { background: rgba(var(--primary-rgb), 0.1); border-left: 4px solid var(--primary); font-weight: 700; color: var(--primary); }
        .toc-item:active { background: var(--surface); }

        /* More Menu Styles */
        .more-menu-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; z-index: 150; }
        .more-menu { position: absolute; top: calc(55px + env(safe-area-inset-top)); right: 15px; width: 180px; background: var(--surface); border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.4); border: 1px solid var(--border); padding: 8px 0; overflow: hidden; animation: menuFade 0.2s ease-out; }
        @keyframes menuFade { from { opacity: 0; transform: translateY(-10px) scale(0.95); } to { opacity: 1; transform: translateY(0) scale(1); } }
        .menu-item { padding: 12px 18px; display: flex; align-items: center; font-size: 0.95rem; cursor: pointer; color: var(--text); }
        .menu-item:active { background: rgba(255,255,255,0.05); }

        /* Modal Styles */
        .modal-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.7); display: flex; align-items: center; justify-content: center; z-index: 300; padding: 20px; backdrop-filter: blur(4px); }
        .modal-content { background: var(--surface); width: 100%; max-width: 320px; border-radius: 20px; padding: 25px; border: 1px solid var(--border); box-shadow: 0 10px 40px rgba(0,0,0,0.5); animation: modalIn 0.3s cubic-bezier(0.34, 1.56, 0.64, 1); }
        @keyframes modalIn { from { transform: scale(0.8); opacity: 0; } to { transform: scale(1); opacity: 1; } }
        .modal-content h4 { margin: 0 0 20px 0; font-size: 1.2rem; text-align: center; }
        .prop-row { display: flex; justify-content: space-between; margin-bottom: 12px; font-size: 0.9rem; color: var(--text-dim); }
        .prop-row span { font-weight: 600; color: var(--text); }
        .modal-close { width: 100%; margin-top: 20px; background: var(--primary); color: var(--bg); border: none; padding: 12px; border-radius: 10px; font-weight: 700; cursor: pointer; }
      `}</style>
        </div>
    );
};

export default App;
