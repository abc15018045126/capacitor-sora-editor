export interface Note {
    id: string;
    title: string;
    content: string;
    time: number;
    isNew?: boolean;
    inTrash?: boolean;
}

export const DIR = 'Notes';
