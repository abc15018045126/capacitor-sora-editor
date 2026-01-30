import React from 'react';

const EditorView: React.FC<any> = ({ onClose }) => {
    return (
        <div style={{ padding: 20 }}>
            <h2>Editor View Placeholder</h2>
            <button onClick={onClose}>Close</button>
        </div>
    );
};

export default EditorView;
