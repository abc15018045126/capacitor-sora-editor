import React from 'react';
import { Icon } from './Icons';
import { Note } from './Types';

interface ListViewProps {
    t: any;
    lang: 'zh' | 'en';
    theme: 'dark' | 'light';
    curGroup: string | null;
    searchQuery: string;
    setSearchQuery: (v: string) => void;
    filteredNotes: Note[];
    selectedIds: string[];
    isSelectMode: boolean;
    setIsSelectMode: (v: boolean) => void;
    setSelectedIds: React.Dispatch<React.SetStateAction<string[]>>;
    onOpenNote: (id: string) => void;
    onSidebarOpen: () => void;
    onSettingsOpen: () => void;
    onCreateNote: () => void;
    onBulkProcess: (action: 'trash' | 'delete' | 'move', target?: string) => void;
    setShowMoveToModal: (v: boolean) => void;
    listRef: React.RefObject<HTMLDivElement | null>;
    listScroll: any;
    handleDrag: (e: any, type: any) => void;
    syncScroll: (type: any) => void;
    timeouts: any;
    drag: any;
    curNote?: Note;
}

const ListView: React.FC<ListViewProps> = ({
    t, lang, theme, curGroup, searchQuery, setSearchQuery, filteredNotes, selectedIds, isSelectMode, setIsSelectMode, setSelectedIds, onOpenNote, onSidebarOpen, onSettingsOpen, onCreateNote, onBulkProcess, setShowMoveToModal, listRef, listScroll, handleDrag, syncScroll, timeouts, drag
}) => {
    const [showSearch, setShowSearch] = React.useState(false);
    return (
        <div className="view">
            <header>
                <div style={{ display: 'flex', alignItems: 'center' }}>
                    {isSelectMode ? (
                        <button className="btn-icon" onClick={() => { setIsSelectMode(false); setSelectedIds([]); }}><Icon d="M6 18L18 6M6 6l12 12" /></button>
                    ) : (
                        <button className="btn-icon" onClick={onSidebarOpen} style={{ marginRight: 10 }}><Icon d="M4 6h16M4 12h16M4 18h16" /></button>
                    )}
                    <h1>{isSelectMode ? t.selectItems.replace('{0}', selectedIds.length.toString()) : (curGroup ? curGroup : (curGroup === '' ? t.uncategorized : t.allNotes))}</h1>
                </div>
                <div style={{ display: 'flex', alignItems: 'center' }}>
                    {!isSelectMode && (
                        <>
                            <button className="btn-icon" onClick={() => setShowSearch(!showSearch)} style={{ marginRight: 5 }}>
                                <Icon d="M21 21l-4.35-4.35M19 11a8 8 0 1 1-16 0 8 8 0 0 1 16 0z" />
                            </button>
                            <button className="btn-icon" onClick={onSettingsOpen}><Icon d="M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z" /></button>
                        </>
                    )}
                </div>
            </header>
            {showSearch && (
                <div className="search-bar-container" style={{ animation: 'slideDown 0.3s ease-out' }}>
                    <input className="search-input" placeholder={t.search} value={searchQuery} onChange={e => setSearchQuery(e.target.value)} autoFocus />
                </div>
            )}
            <div className="list-container" ref={listRef} onScroll={() => syncScroll('list')}>
                {filteredNotes.length === 0 ? <div style={{ textAlign: 'center', padding: 40, color: 'var(--text-dim)' }}>{t.noNotes}</div> :
                    filteredNotes.map(n => {
                        const isSel = selectedIds.includes(n.id);
                        return (
                            <div key={n.id} className={`note-card ${isSel ? 'selected' : ''}`}
                                onClick={() => {
                                    if (isSelectMode) setSelectedIds(p => isSel ? p.filter(x => x !== n.id) : [...p, n.id]);
                                    else onOpenNote(n.id);
                                }}
                                onTouchStart={() => {
                                    if (!isSelectMode) {
                                        timeouts.current.lp = window.setTimeout(() => {
                                            setIsSelectMode(true);
                                            setSelectedIds([n.id]);
                                        }, 600);
                                    }
                                }}
                                onTouchMove={(e) => {
                                    if (timeouts.current.lp) {
                                        const touch = e.touches[0];
                                        if (!drag.current.lpStart) {
                                            drag.current.lpStart = { x: touch.clientX, y: touch.clientY };
                                        } else {
                                            const dx = Math.abs(touch.clientX - drag.current.lpStart.x);
                                            const dy = Math.abs(touch.clientY - drag.current.lpStart.y);
                                            if (dx > 10 || dy > 10) {
                                                window.clearTimeout(timeouts.current.lp);
                                                timeouts.current.lp = 0;
                                            }
                                        }
                                    }
                                }}
                                onTouchEnd={() => {
                                    window.clearTimeout(timeouts.current.lp);
                                    timeouts.current.lp = 0;
                                    drag.current.lpStart = null;
                                }}
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
            {!isSelectMode ? <button id="fab" onClick={onCreateNote}>+</button> : (
                <div className="selection-toolbar">
                    <button onClick={() => setSelectedIds(selectedIds.length === filteredNotes.length ? [] : filteredNotes.map(n => n.id))}>{selectedIds.length === filteredNotes.length ? t.deselectAll : t.selectAll}</button>
                    <button onClick={() => setShowMoveToModal(true)} disabled={!selectedIds.length}>{t.moveTo}</button>
                    <button style={{ color: '#ff4d4f' }} onClick={() => onBulkProcess(curGroup === '__TRASH__' ? 'delete' : 'trash')} disabled={!selectedIds.length}>{curGroup === '__TRASH__' ? t.batchDelete : t.batchTrash}</button>
                </div>
            )}
        </div>
    );
};

export default ListView;
