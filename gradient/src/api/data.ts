import axios, { AxiosResponse } from 'axios';
import { ItemType } from './data_types';

const BASE_URL = process.env.REACT_APP_API_BASE_URL!;
const API_KEY = process.env.REACT_APP_API_KEY!;

const endpoint = (name: string) => `${BASE_URL}/data/${name}`;
const headers = { 'X-Api-Key': API_KEY };

export const getItems = (): Promise<AxiosResponse<ItemType[]>> =>
  axios.get(endpoint('items'), { headers });
